package com.ctreactkotlintutorial

import android.app.Application
import android.content.Context
import android.os.Handler
import com.facebook.react.ReactApplication
import com.clevertap.android.sdk.pushnotification.CTPushNotificationListener
import com.facebook.react.ReactNativeHost
import com.facebook.react.ReactPackage
import com.facebook.react.PackageList
import com.clevertap.android.sdk.ActivityLifecycleCallback
import com.clevertap.android.sdk.CleverTapAPI
import com.facebook.soloader.SoLoader
import com.ctreactkotlintutorial.MainApplication
import android.os.Looper
import android.util.Log
import com.facebook.react.ReactInstanceManager
import com.facebook.react.bridge.ReactContext
import com.facebook.react.ReactInstanceManager.ReactInstanceEventListener
import com.facebook.react.bridge.Arguments
import com.facebook.react.modules.core.DeviceEventManagerModule.RCTDeviceEventEmitter
import com.facebook.react.bridge.WritableMap
import org.json.JSONObject
import java.lang.reflect.InvocationTargetException
import java.util.HashMap

class MainApplication : Application(), ReactApplication, CTPushNotificationListener {
    private val mReactNativeHost: ReactNativeHost = object : ReactNativeHost(this) {
        override fun getUseDeveloperSupport(): Boolean {
            return BuildConfig.DEBUG
        }

        override fun getPackages(): List<ReactPackage> {
            // Packages that cannot be autolinked yet can be added manually here, for example:
            // packages.add(new MyReactNativePackage());
            return PackageList(this).packages
        }

        override fun getJSMainModuleName(): String {
            return "index"
        }
    }

    override fun getReactNativeHost(): ReactNativeHost {
        return mReactNativeHost
    }

    override fun onCreate() {
        ActivityLifecycleCallback.register(this)
        CleverTapAPI.setDebugLevel(3)
        super.onCreate()
        SoLoader.init(this,  /* native exopackage */false)
        CleverTapAPI.getDefaultInstance(this)!!.ctPushNotificationListener = this
        initializeFlipper(this, reactNativeHost.reactInstanceManager)
    }

    override fun onNotificationClickedPayloadReceived(payload: HashMap<String, Any>) {
        Log.e("MainApplication", "onNotificationClickedPayloadReceived called")
        val CLEVERTAP_PUSH_NOTIFICATION_CLICKED = "CleverTapPushNotificationClicked"
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            // Construct and load our normal React JS code bundle
            val mReactInstanceManager = (applicationContext as ReactApplication).reactNativeHost.reactInstanceManager
            val context = mReactInstanceManager.currentReactContext
            // If it’s constructed, send a notification
            if (context != null) {
                sendEvent(CLEVERTAP_PUSH_NOTIFICATION_CLICKED, getWritableMapFromMap(payload), context)
            } else {
                // Otherwise wait for construction, then send the notification
                mReactInstanceManager.addReactInstanceEventListener(
                        object : ReactInstanceEventListener {
                            override fun onReactContextInitialized(context: ReactContext) {
                                sendEvent(CLEVERTAP_PUSH_NOTIFICATION_CLICKED, getWritableMapFromMap(payload), context)
                                mReactInstanceManager.removeReactInstanceEventListener(this)
                            }
                        })
                if (!mReactInstanceManager.hasStartedCreatingInitialContext()) {
                    mReactInstanceManager.createReactContextInBackground()
                }
            }
        }
    }

    private fun sendEvent(eventName: String, params: Any, context: ReactContext) {
        try {
            context.getJSModule(RCTDeviceEventEmitter::class.java).emit(eventName, params)
        } catch (t: Throwable) {
            Log.e("TAG", t.localizedMessage)
        }
    }

    private fun getWritableMapFromMap(var1: Map<String, Any>?): WritableMap {
        val extras = var1?.let { JSONObject(it) } ?: JSONObject()
        val extrasParams = Arguments.createMap()
        val extrasKeys: Iterator<*> = extras.keys()
        while (extrasKeys.hasNext()) {
            var key: String? = null
            var value: String? = null
            try {
                key = extrasKeys.next().toString()
                value = extras[key].toString()
            } catch (t: Throwable) {
                Log.e("TAG", t.localizedMessage)
            }
            if (key != null && value != null) {
                extrasParams.putString(key, value)
            }
        }
        return extrasParams
    }

    companion object {
        /**
         * Loads Flipper in React Native templates. Call this in the onCreate method with something like
         * initializeFlipper(this, getReactNativeHost().getReactInstanceManager());
         *
         * @param context
         * @param reactInstanceManager
         */
        private fun initializeFlipper(
                context: Context, reactInstanceManager: ReactInstanceManager) {
            if (BuildConfig.DEBUG) {
                try {
                    /*
         We use reflection here to pick up the class that initializes Flipper,
        since Flipper library is not available in release mode
        */
                    val aClass = Class.forName("com.ctreactkotlintutorial.ReactNativeFlipper")
                    aClass.getMethod("initializeFlipper", Context::class.java, ReactInstanceManager::class.java)
                            .invoke(null, context, reactInstanceManager)
                } catch (e: ClassNotFoundException) {
                    e.printStackTrace()
                } catch (e: NoSuchMethodException) {
                    e.printStackTrace()
                } catch (e: IllegalAccessException) {
                    e.printStackTrace()
                } catch (e: InvocationTargetException) {
                    e.printStackTrace()
                }
            }
        }
    }
}