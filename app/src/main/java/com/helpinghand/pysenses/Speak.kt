package com.helpinghand.pysenses

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.*

class Speak(context: Context) {
    private var t1: TextToSpeech? = null
    init {
        t1 = TextToSpeech(context,
            TextToSpeech.OnInitListener { status ->
                if (status != TextToSpeech.ERROR) {
                    t1?.language = Locale("en", "IN")
                }
            })
        t1!!.setSpeechRate(2f)
    }

    fun talk(charSequence: CharSequence) {
        if (!t1!!.isSpeaking) {
            t1!!.speak(charSequence, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    fun setSpeechRate(value:Float){
        t1!!.setSpeechRate(value)
    }

    fun status(): Boolean {
        return t1!!.isSpeaking
    }

    fun close() {
        t1!!.shutdown()
    }
}