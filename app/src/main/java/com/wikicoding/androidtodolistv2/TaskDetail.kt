package com.wikicoding.androidtodolistv2

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import com.wikicoding.androidtodolistv2.constants.Constants
import com.wikicoding.androidtodolistv2.databinding.ActivityTaskDetailBinding
import com.wikicoding.androidtodolistv2.model.TaskEntity

class TaskDetail : AppCompatActivity() {
    private var binding: ActivityTaskDetailBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskDetailBinding.inflate(LayoutInflater.from(this))
        setContentView(binding!!.root)

        val toolbar = binding!!.toolbarTaskDetail
        setSupportActionBar(toolbar)
        supportActionBar!!.title = "Task Detail"
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        if (intent.hasExtra(Constants.INTENT_DATA)) {
            val instanceOfTask = intent.getSerializableExtra(Constants.INTENT_DATA) as TaskEntity

            binding!!.tvDescriptionText.text = instanceOfTask.title
            if (instanceOfTask.completed) {
                binding!!.tvCompletedText.text = "Completed Task"
            } else {
                binding!!.tvCompletedText.text = "Task is Incomplete"
            }

            binding!!.tvCreatedAtText.text = instanceOfTask.timestamp
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}