package com.gdd.rankingfilter.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.viewbinding.ViewBinding
import com.gdd.rankingfilter.R
import com.gdd.rankingfilter.extention.checkIfFragmentAttached

abstract class BaseFragment<BINDING : ViewBinding>(private val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> BINDING) : Fragment() {

    lateinit var binding : BINDING
    private val navController by lazy { findNavController() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = bindingInflater(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initData()
        setUpView()
        setUpListener()
    }

    abstract fun initData()
    abstract fun setUpView()
    abstract fun setUpListener()

    /**
     * Navigate to other fragment
     * @param id Fragment id to move to
     * @param inclusive remove current fragment from backstack
     */
    protected fun navigateTo(id: Int, inclusive: Boolean = false) {
        checkIfFragmentAttached {
            val controller = parentFragment?.findNavController() ?: navController
            controller.navigate(
                id,
                null,
                buildNavOptions(inclusive)
            )
        }
    }

    /**
     * Navigate using Safe Args action from child fragment to parent navigation
     * @param action NavDirections action with type-safe arguments
     * @param inclusive remove current fragment from backstack
     */
    protected fun navigateWithAction(action: NavDirections, inclusive: Boolean = false) {
        checkIfFragmentAttached {
            val controller = parentFragment?.findNavController() ?: navController
            controller.navigate(
                action,
                buildNavOptions(inclusive)
            )
        }
    }

    protected fun navigateBack() {
        checkIfFragmentAttached {
            val controller = parentFragment?.findNavController() ?: navController
            // Try to pop one entry from back stack
            val popped = controller.popBackStack()
            if (!popped) {
                // If nothing popped (no previous), try navigateUp() as fallback
                controller.navigateUp()
            }
        }
    }

    /**
     * Send a result to the previous fragment on the back stack using SavedStateHandle,
     * then pop back to it. If there's no previous entry, will either navigate using
     * fallbackAction (if provided) or call navigateUp().
     *
     * @param key key for savedStateHandle
     * @param value value to send (can be Int, String, Parcelable, etc)
     * @param inclusive if true and fallbackAction is used, popUpTo current destination (keeps behavior consistent with navigateWithAction)
     * @param fallbackAction optional NavDirections to navigate to if there's no previous entry
     */
    protected fun <T> navigateBackWithResult(
        key: String,
        value: T,
        inclusive: Boolean = false,
        fallbackAction: NavDirections? = null
    ) {
        checkIfFragmentAttached {
            val controller = parentFragment?.findNavController() ?: navController
            val prevEntry = controller.previousBackStackEntry
            if (prevEntry != null) {
                // send result to previous fragment
                prevEntry.savedStateHandle.set(key, value)
                // go back to previous fragment
                controller.popBackStack()
            } else {
                // no previous fragment: either navigate using fallback or go up
                if (fallbackAction != null) {
                    controller.navigate(fallbackAction, buildNavOptions(inclusive))
                } else {
                    controller.navigateUp()
                }
            }
        }
    }

    /**
     * Pop back to a specific destination in the back stack.
     * @param destinationId id of the destination to pop to
     * @param inclusive whether to also remove the destination itself
     */
    protected fun popBackStackTo(destinationId: Int, inclusive: Boolean = false) {
        checkIfFragmentAttached {
            val controller = parentFragment?.findNavController() ?: navController
            controller.popBackStack(destinationId, inclusive)
        }
    }


    private fun buildNavOptions(inclusive: Boolean): NavOptions {
        return NavOptions.Builder().apply {
            val currentDestination = findNavController().currentDestination?.id
            if (inclusive && currentDestination != null) {
                setPopUpTo(currentDestination, true)
            }
            setEnterAnim(R.anim.no_animation)
            setExitAnim(R.anim.no_animation)
            setPopEnterAnim(R.anim.no_animation)
            setPopExitAnim(R.anim.slide_out_right)
        }.build()
    }
}