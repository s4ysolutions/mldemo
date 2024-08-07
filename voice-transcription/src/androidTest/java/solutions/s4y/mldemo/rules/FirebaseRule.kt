package solutions.s4y.mldemo.rules

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.junit.rules.MethodRule
import org.junit.runners.model.FrameworkMethod
import org.junit.runners.model.Statement
import solutions.s4y.mldemo.googleServicesOptionsBuilder

private var initialized = false
class FirebaseRule: MethodRule {
    override fun apply(base: Statement, method: FrameworkMethod?, target: Any?): Statement {
        if (!initialized) {
            val builder: FirebaseOptions.Builder = googleServicesOptionsBuilder()
            val options = builder.build()
            val app = ApplicationProvider.getApplicationContext<Application>()
            FirebaseApp.initializeApp(app, options, "[DEFAULT]")
            initialized = true
        }
        return object : Statement() {
            override fun evaluate() {
                base.evaluate()
            }
        }
    }
}