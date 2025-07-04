package com.gdd.rankingfilter.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
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
    protected fun navigateTo(
        id: Int,
        inclusive: Boolean = false,
    ) {
        checkIfFragmentAttached {
            navController.navigate(
                id,
                null,
                buildNavOptions(inclusive)
            )
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