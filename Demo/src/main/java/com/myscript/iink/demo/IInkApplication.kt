//AI STATEMENT: AI was not used in this. This file was imported from https://github.com/MyScript/interactive-ink-examples-android
//into this Android SDK and then edited by our team

package com.myscript.iink.demo
import android.app.Application
import com.myscript.iink.demo.di.DemoModule

class IInkApplication : Application() {

    companion object {
        lateinit var DemoModule: DemoModule
    }

    override fun onCreate() {
        super.onCreate()
        DemoModule = DemoModule(this)
    }

    override fun onTerminate() {
        DemoModule.close()
        super.onTerminate()
    }
}
