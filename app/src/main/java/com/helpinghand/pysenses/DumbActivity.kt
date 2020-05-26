package com.helpinghand.pysenses

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.nex3z.togglebuttongroup.button.LabelToggle
import kotlinx.android.synthetic.main.activity_dumb.*

class DumbActivity : AppCompatActivity() {

    private lateinit var tts:Speak

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_dumb)
        tts=Speak(this)
        tts.setSpeechRate(1f)
        val frequentWords = listOf(
            "the",
            "and",
            "a",
            "that",
            "I",
            "it",
            "not",
            "he",
            "as",
            "you",
            "this",
            "but",
            "his",
            "they",
            "her",
            "she",
            "or",
            "an",
            "will",
            "my",
            "one",
            "all",
            "would",
            "there",
            "their",
            "to",
            "of",
            "in",
            "for",
            "on",
            "with",
            "at",
            "by",
            "from",
            "up",
            "about",
            "into",
            "over",
            "after",
            "time",
            "person",
            "year",
            "way",
            "day",
            "thing",
            "man",
            "world",
            "life",
            "hand",
            "part",
            "child",
            "eye",
            "woman",
            "place",
            "work",
            "week",
            "case",
            "point",
            "government",
            "company",
            "number",
            "group",
            "problem",
            "fact",
            "be",
            "have",
            "do",
            "say",
            "get",
            "make",
            "go",
            "know",
            "take",
            "see",
            "come",
            "think",
            "look",
            "want",
            "give",
            "use",
            "find",
            "tell",
            "ask",
            "work",
            "seem",
            "feel",
            "try",
            "leave",
            "call",
            "good",
            "new",
            "first",
            "last",
            "long",
            "great",
            "little",
            "own",
            "other",
            "old",
            "right",
            "big",
            "high",
            "different",
            "small",
            "large",
            "next",
            "early",
            "young",
            "important",
            "few",
            "public",
            "bad",
            "same",
            "able"
        )
        val params=ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT)
        for (i in frequentWords.indices) {
            val btn = LabelToggle(this)
            btn.id = 2000 + i
            btn.text=frequentWords[i]
            btn.textSize = 18f
            btn.setOnClickListener {
                btn.isChecked=false
                translatebox.editableText.append(btn.text as String +" ")
            }
            suggestiongrp.addView(btn,params)
        }
        translatebtn.setOnClickListener { tts.talk(translatebox.editableText) }
    }

    override fun onDestroy() {
        super.onDestroy()
        when(!tts.status()){
            true->tts.close()
        }
    }
}
