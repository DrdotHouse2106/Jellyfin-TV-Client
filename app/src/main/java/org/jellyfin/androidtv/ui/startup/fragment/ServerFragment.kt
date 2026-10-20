package org.jellyfin.androidtv.ui.startup.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.tvclient.TvClientAvatars
import org.jellyfin.androidtv.auth.model.ApiClientErrorLoginState
import org.jellyfin.androidtv.auth.model.AuthenticatedState
import org.jellyfin.androidtv.auth.model.AuthenticatingState
import org.jellyfin.androidtv.auth.model.PrivateUser
import org.jellyfin.androidtv.auth.model.RequireSignInState
import org.jellyfin.androidtv.auth.model.Server
import org.jellyfin.androidtv.auth.model.ServerUnavailableState
import org.jellyfin.androidtv.auth.model.ServerVersionNotSupported
import org.jellyfin.androidtv.auth.model.User
import org.jellyfin.androidtv.auth.repository.AuthenticationRepository
import org.jellyfin.androidtv.auth.repository.ServerRepository
import org.jellyfin.androidtv.auth.repository.ServerUserRepository
import org.jellyfin.androidtv.data.service.BackgroundService
import org.jellyfin.androidtv.databinding.FragmentServerBinding
import org.jellyfin.androidtv.ui.ServerButtonView
import org.jellyfin.androidtv.ui.card.UserCardView
import org.jellyfin.androidtv.ui.startup.StartupViewModel
import org.jellyfin.androidtv.util.ListAdapter
import org.jellyfin.androidtv.util.MarkdownRenderer
import org.jellyfin.androidtv.util.popupMenu
import org.jellyfin.androidtv.util.showIfNotEmpty
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class ServerFragment : Fragment() {
	companion object {
		const val ARG_SERVER_ID = "server_id"
	}

	private val startupViewModel: StartupViewModel by activityViewModel()
	private val markdownRenderer: MarkdownRenderer by inject()
	private val authenticationRepository: AuthenticationRepository by inject()
	private val serverUserRepository: ServerUserRepository by inject()
	private val backgroundService: BackgroundService by inject()
	private var _binding: FragmentServerBinding? = null
	private val binding get() = _binding!!

	private val serverIdArgument get() = arguments?.getString(ARG_SERVER_ID)?.ifBlank { null }?.toUUIDOrNull()

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		val server = serverIdArgument?.let(startupViewModel::getServer)

		if (server == null) {
			navigateFragment<SelectServerFragment>(keepToolbar = true, keepHistory = false)
			return null
		}

		_binding = FragmentServerBinding.inflate(inflater, container, false)

		val userAdapter = UserAdapter(requireContext(), server, startupViewModel, authenticationRepository, serverUserRepository)
		userAdapter.onItemPressed = { user ->
			startupViewModel.authenticate(server, user).onEach { state ->
				when (state) {
					// Jellyfin-TV-Client: sichtbare Rückmeldung, solange der Server antwortet
					AuthenticatingState -> Toast.makeText(context, getString(R.string.tvclient_signing_in, user.name), Toast.LENGTH_SHORT).show()
					AuthenticatedState -> Unit
					// Actions
					RequireSignInState -> navigateFragment<UserLoginFragment>(bundleOf(
						UserLoginFragment.ARG_SERVER_ID to server.id.toString(),
						UserLoginFragment.ARG_USERNAME to user.name,
					))
					// Errors
					ServerUnavailableState,
					is ApiClientErrorLoginState -> Toast.makeText(context, R.string.server_connection_failed, Toast.LENGTH_LONG).show()

					is ServerVersionNotSupported -> Toast.makeText(
						context,
						getString(
							R.string.server_issue_outdated_version,
							state.server.version,
							ServerRepository.recommendedServerVersion.toString()
						),
						Toast.LENGTH_LONG
					).show()
				}
			}.launchIn(lifecycleScope)
		}
		// Jellyfin-TV-Client: Kachel „Profil hinzufügen“ hinter den Profilen
		val addProfileAdapter = AddProfileAdapter(requireContext()) {
			navigateFragment<UserLoginFragment>(
				args = bundleOf(
					UserLoginFragment.ARG_SERVER_ID to server.id.toString(),
					UserLoginFragment.ARG_USERNAME to null
				)
			)
		}
		binding.users.adapter = ConcatAdapter(userAdapter, addProfileAdapter)
		binding.manageProfilesButton.setOnClickListener { showManageMenu(server) }

		startupViewModel.users
			.flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
			.onEach { users ->
				userAdapter.items = users

				// Jellyfin-TV-Client: die Kachel „Profil hinzufügen“ ist immer da – kein Hinweistext nötig
				binding.users.isFocusable = true
				// Jellyfin-TV-Client: Profile mittig anzeigen wie bei Netflix (Karte = 178dp breit)
				binding.users.post {
					val grid = _binding?.users ?: return@post
					val cardWidth = (178 * resources.displayMetrics.density).toInt()
					val padding = ((grid.width - cardWidth * (users.size + 1)) / 2).coerceAtLeast(0)
					grid.setPadding(padding, 0, padding, 0)
				}
				binding.noUsersWarning.isVisible = false
				binding.root.requestFocus()
			}.launchIn(viewLifecycleOwner.lifecycleScope)

		startupViewModel.loadUsers(server)

		// Jellyfin-TV-Client: nach neuem Profilbild die Profile neu laden
		childFragmentManager.setFragmentResultListener(TvClientAvatars.RESULT_KEY, viewLifecycleOwner) { _, _ ->
			startupViewModel.loadUsers(server)
		}

		onServerChange(server)

		lifecycleScope.launch {
			lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
				val updated = startupViewModel.updateServer(server)
				if (updated) startupViewModel.getServer(server.id)?.let(::onServerChange)
			}
		}

		return binding.root
	}

	private var avatarsAvailable: Boolean? = null

	/** Jellyfin-TV-Client: „Profile verwalten“ – Profilbild wählen, abmelden, entfernen, Server wechseln. */
	private fun showManageMenu(server: Server) = viewLifecycleOwner.lifecycleScope.launch {
		val users = startupViewModel.users.value
		// Profilbilder nur anbieten, wenn der Server sie kennt (Plugin Local Recommendations)
		val token = users.filterIsInstance<PrivateUser>().firstNotNullOfOrNull { it.accessToken }
		val avatars = avatarsAvailable ?: (token != null && TvClientAvatars.isAvailable(server.address, token)).also { avatarsAvailable = it }
		if (_binding == null) return@launch
		popupMenu(requireContext(), binding.manageProfilesButton) {
			users.filterIsInstance<PrivateUser>().forEach { user ->
				val userToken = user.accessToken
				if (avatars && userToken != null) item(getString(R.string.tvclient_avatar_choose_for, user.name)) {
					TvClientAvatars.show(childFragmentManager, server.address, userToken, user.name)
				}
				if (user.accessToken != null) item(getString(R.string.tvclient_profile_sign_out, user.name)) {
					authenticationRepository.logout(user)
				}
				item(getString(R.string.tvclient_profile_remove, user.name)) {
					serverUserRepository.deleteStoredUser(user)
					startupViewModel.loadUsers(server)
				}
			}
			item(getString(R.string.tvclient_switch_server, server.name)) {
				navigateFragment<SelectServerFragment>(keepToolbar = true)
			}
		}.showIfNotEmpty()
	}

	override fun onPause() {
		super.onPause()
		requireActivity().findViewById<View>(R.id.content_view)?.background = null
	}

	override fun onDestroyView() {
		super.onDestroyView()

		_binding = null
	}

	private fun onServerChange(server: Server) {
		binding.loginDisclaimer.text = server.loginDisclaimer?.let { markdownRenderer.toMarkdownSpanned(it) }

		binding.serverButton.apply {
			state = ServerButtonView.State.EDIT
			name = server.name
			address = server.address
			version = server.version
		}

		binding.addUserButton.setOnClickListener {
			navigateFragment<UserLoginFragment>(
				args = bundleOf(
					UserLoginFragment.ARG_SERVER_ID to server.id.toString(),
					UserLoginFragment.ARG_USERNAME to null
				)
			)
		}

		binding.serverButton.setOnClickListener {
			navigateFragment<SelectServerFragment>(keepToolbar = true)
		}

		if (!server.versionSupported) {
			binding.notification.isVisible = true
			binding.notification.text = getString(
				R.string.server_unsupported_notification,
				server.version,
				ServerRepository.recommendedServerVersion.toString(),
			)
		} else if (!server.setupCompleted) {
			binding.notification.isVisible = true
			binding.notification.text = getString(R.string.server_setup_incomplete)
		} else {
			binding.notification.isGone = true
		}
	}

	private inline fun <reified F : Fragment> navigateFragment(
		args: Bundle = bundleOf(),
		keepToolbar: Boolean = false,
		keepHistory: Boolean = true,
	) {
		requireActivity()
			.supportFragmentManager
			.commit {
				if (keepToolbar) {
					replace<StartupToolbarFragment>(R.id.content_view)
					add<F>(R.id.content_view, null, args)
				} else {
					replace<F>(R.id.content_view, null, args)
				}

				if (keepHistory) addToBackStack(null)
			}
	}

	override fun onResume() {
		super.onResume()

		startupViewModel.reloadStoredServers()
		backgroundService.clearBackgrounds()
		// Jellyfin-TV-Client: schwarzer Hintergrund wie bei Netflix (auch hinter der Kopfzeile)
		requireActivity().findViewById<View>(R.id.content_view)?.setBackgroundColor(android.graphics.Color.BLACK)

		val server = serverIdArgument?.let(startupViewModel::getServer)
		if (server != null) startupViewModel.loadUsers(server)
		else navigateFragment<SelectServerFragment>(keepToolbar = true)
	}

	private class UserAdapter(
		private val context: Context,
		private val server: Server,
		private val startupViewModel: StartupViewModel,
		private val authenticationRepository: AuthenticationRepository,
		private val serverUserRepository: ServerUserRepository,
	) : ListAdapter<User, UserAdapter.ViewHolder>() {
		var onItemPressed: (User) -> Unit = {}

		override fun areItemsTheSame(old: User, new: User): Boolean = old.id == new.id

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
			val cardView = UserCardView(context)

			return ViewHolder(cardView)
		}

		override fun onBindViewHolder(holder: ViewHolder, user: User) {
			holder.cardView.name = user.name
			holder.cardView.image = startupViewModel.getUserImage(server, user)

			holder.cardView.setPopupMenu {
				// Logout button
				if (user is PrivateUser && user.accessToken != null) {
					item(context.getString(R.string.lbl_sign_out)) {
						authenticationRepository.logout(user)
					}
				}

				// Remove button
				if (user is PrivateUser) {
					item(context.getString(R.string.lbl_remove)) {
						serverUserRepository.deleteStoredUser(user)
						startupViewModel.loadUsers(server)
					}
				}
			}

			holder.cardView.setOnClickListener {
				onItemPressed(user)
			}
		}

		private class ViewHolder(
			val cardView: UserCardView,
		) : RecyclerView.ViewHolder(cardView)
	}

	private class AddProfileAdapter(
		private val context: Context,
		private val onPressed: () -> Unit,
	) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
		override fun getItemCount() = 1

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
			val cardView = UserCardView(context).apply {
				addTile = true
				name = context.getString(R.string.tvclient_add_profile)
				setOnClickListener { onPressed() }
			}
			return object : RecyclerView.ViewHolder(cardView) {}
		}

		override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) = Unit
	}
}

