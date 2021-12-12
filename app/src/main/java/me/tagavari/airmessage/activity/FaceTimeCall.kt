package me.tagavari.airmessage.activity

import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.IntDef
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.disposables.CompositeDisposable
import me.tagavari.airmessage.R
import me.tagavari.airmessage.fragment.FragmentCallPending
import me.tagavari.airmessage.redux.ReduxEmitterNetwork
import me.tagavari.airmessage.redux.ReduxEventFaceTime

class FaceTimeCall : AppCompatActivity(R.layout.activity_facetimecall) {
	//State
	private val viewModel: ActivityViewModel by viewModels()
	
	//A composite disposable, valid for the lifecycle of this call
	private val compositeDisposableCalls = CompositeDisposable()
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		
		if(savedInstanceState == null) {
			//Read the parameters
			@Type val type = intent.extras!!.getInt(PARAM_TYPE)
			val participants = intent.extras!!.getStringArrayList(PARAM_PARTICIPANTS)
			
			//Set the initial state
			viewModel.state = if(type == Type.outgoing) State.outgoing else State.incoming
			
			//Add the fragment
			supportFragmentManager.commit {
				setReorderingAllowed(true)
				add<FragmentCallPending>(
					R.id.content,
					args = bundleOf(
						FragmentCallPending.PARAM_STATE to FragmentCallPending.State.outgoing,
						FragmentCallPending.PARAM_PARTICIPANTS to participants
					)
				)
			}
		}
		
		//Subscribe to updates
		//viewModel.state.observe(this, this::applyState)
		if(viewModel.state != State.rejected) {
			compositeDisposableCalls.add(
				ReduxEmitterNetwork.faceTimeUpdateSubject.subscribe(this::handleFaceTimeUpdate)
			)
		}
	}
	
	override fun onDestroy() {
		super.onDestroy()
		
		//Make sure we clean up any listeners
		compositeDisposableCalls.dispose()
	}
	
	private fun updateStateRejected() {
		//Update the state to rejected
		viewModel.state = State.rejected
		
		//Update the fragment
		val fragment = supportFragmentManager.findFragmentById(R.id.content) as FragmentCallPending
		fragment.updateState(FragmentCallPending.State.disconnected)
		
		//Clean up, call lifecycle is finished
		compositeDisposableCalls.dispose()
	}
	
	private fun updateStateError(errorDetails: String?) {
		//Update the state to rejected
		updateStateRejected()
		
		//Show an error dialog
		val fragment = supportFragmentManager.findFragmentById(R.id.content) as FragmentCallPending
		fragment.showError(errorDetails)
	}
	
	private fun updateStateCalling(faceTimeLink: String) {
		//Update the state to calling
		viewModel.state = State.calling
		
		//TODO swap to the calling fragment
	}
	
	private fun handleFaceTimeUpdate(update: ReduxEventFaceTime) {
		if(viewModel.state == State.outgoing) {
			when(update) {
				is ReduxEventFaceTime.OutgoingAccepted -> updateStateCalling(update.faceTimeLink)
				is ReduxEventFaceTime.OutgoingRejected -> updateStateRejected()
				is ReduxEventFaceTime.OutgoingError -> updateStateError(update.errorDetails)
			}
		} else if(viewModel.state == State.incoming) {
			when(update) {
				is ReduxEventFaceTime.IncomingHandled -> updateStateCalling(update.faceTimeLink)
				is ReduxEventFaceTime.IncomingHandleError -> updateStateError(update.errorDetails)
			}
		}
	}
	
	class ActivityViewModel : ViewModel() {
		@State var state: Int = State.outgoing
	}
	
	@Retention(AnnotationRetention.SOURCE)
	@IntDef(Type.outgoing, Type.incoming)
	annotation class Type {
		companion object {
			const val outgoing = 0
			const val incoming = 1
		}
	}
	
	@Retention(AnnotationRetention.SOURCE)
	@IntDef(State.outgoing, State.incoming, State.rejected, State.calling)
	private annotation class State {
		companion object {
			const val outgoing = 0
			const val incoming = 1
			const val rejected = 3
			const val calling = 4
		}
	}
	
	companion object {
		const val PARAM_TYPE = "type"
		const val PARAM_PARTICIPANTS = "participants"
	}
}