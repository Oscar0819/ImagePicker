package com.milet0819.notificationtest.common.utils

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import javax.inject.Inject

private const val loggerTag = "ComponentLogger"

private inline val <T : Any> T.javaClassName: String
    get() = javaClass.name

private fun Activity.printLifecycle(lifecycleScope: String) {
    Log.d(loggerTag, "[Activity] $lifecycleScope - ${javaClassName}(${hashCode()})")
}

private fun Fragment.printLifeCycle(lifecycleScope: String) {
    Log.d(loggerTag, "[Fragment] $lifecycleScope - ${javaClassName}(${hashCode()})")
}

class ComponentLogger @Inject constructor() {
    fun initialize(application: Application) {
        application.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                    activity.printLifecycle("onCreate")
                    handleActivity(activity)
                }

                override fun onActivityStarted(activity: Activity) {
                    activity.printLifecycle("onStart")
                }

                override fun onActivityResumed(activity: Activity) {
                    activity.printLifecycle("onResume")
                }

                override fun onActivityPaused(activity: Activity) {
                    activity.printLifecycle("onPause")
                }

                override fun onActivityStopped(activity: Activity) {
                    activity.printLifecycle("onStop")
                }

                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
                    activity.printLifecycle("onSaveInstance")
                }

                override fun onActivityDestroyed(activity: Activity) {
                    activity.printLifecycle("onDestroy")
                }

            }
        )
    }

    private fun handleActivity(activity: Activity) {
        if (activity is FragmentActivity) {
            activity.supportFragmentManager.registerFragmentLifecycleCallbacks(
                object : FragmentManager.FragmentLifecycleCallbacks() {
                    override fun onFragmentPreAttached(
                        fm: FragmentManager,
                        f: Fragment,
                        context: Context
                    ) { }

                    override fun onFragmentViewCreated(
                        fm: FragmentManager,
                        f: Fragment,
                        v: View,
                        savedInstanceState: Bundle?
                    ) {
                        super.onFragmentViewCreated(fm, f, v, savedInstanceState)
                        f.printLifeCycle("onViewCreated")
                    }

                    override fun onFragmentStarted(fm: FragmentManager, f: Fragment) {
                        super.onFragmentStarted(fm, f)
                        f.printLifeCycle("onStart")
                    }

                    override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
                        super.onFragmentResumed(fm, f)
                        f.printLifeCycle("onResume")
                    }

                    override fun onFragmentPaused(fm: FragmentManager, f: Fragment) {
                        super.onFragmentPaused(fm, f)
                        f.printLifeCycle("onPause")
                    }

                    override fun onFragmentStopped(fm: FragmentManager, f: Fragment) {
                        super.onFragmentStopped(fm, f)
                        f.printLifeCycle("onStop")
                    }

                    override fun onFragmentViewDestroyed(fm: FragmentManager, f: Fragment) {
                        super.onFragmentViewDestroyed(fm, f)
                        f.printLifeCycle("onViewDestroy")
                    }

                    override fun onFragmentDestroyed(fm: FragmentManager, f: Fragment) {
                        super.onFragmentDestroyed(fm, f)
                        f.printLifeCycle("onDestroy")
                    }
                }, true
            )
        }
    }
}