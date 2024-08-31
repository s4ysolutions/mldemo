package solutions.s4y.calculator

class NativeLib {

    /**
     * A native method that is implemented by the 'calculator' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {
        // Used to load the 'calculator' library on application startup.
        init {
            System.loadLibrary("calculator")
        }
    }
}