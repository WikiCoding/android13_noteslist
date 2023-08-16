package com.wikicoding.androidtodolistv2

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.speech.RecognizerIntent
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import android.widget.Toolbar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.wikicoding.androidtodolistv2.adapter.MainActivityAdapter
import com.wikicoding.androidtodolistv2.dao.TaskApp
import com.wikicoding.androidtodolistv2.dao.TaskDao
import com.wikicoding.androidtodolistv2.databinding.ActivityMainBinding
import com.wikicoding.androidtodolistv2.databinding.DialogUpdateBinding
import com.wikicoding.androidtodolistv2.model.TaskEntity
import com.wikicoding.explorelog.utils.SwipeToDeleteCallback
import com.wikicoding.explorelog.utils.SwipeToEditCallback
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {
    private var binding: ActivityMainBinding? = null
    private var list: ArrayList<TaskEntity>? = null
    lateinit var oldList: ArrayList<TaskEntity>
    lateinit var oldItem: TaskEntity
    lateinit var dao: TaskDao
    lateinit var ibMic: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        /** Changing the ActionBar title **/
        title = "Notes List"
        setContentView(binding!!.root)

        /** disabling night mode **/
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        ibMic = binding!!.ibMic

        /** needed to add to AndroidManifest <application android:name android:name=".dao.TaskApp" **/
        dao = (application as TaskApp).db.taskDao()

        getTodos()

        binding!!.btnAddTodo.setOnClickListener {
            addTodo()
        }

        /** Speech Recognition Start **/
        val launchActivityForResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val res: ArrayList<String> =
                        result.data!!.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS) as ArrayList<String>

                    binding!!.etTodo.setText(Objects.requireNonNull(res)[0])
                }
            }

        ibMic.setOnClickListener {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)

            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to text")

            try {
                launchActivityForResult.launch(intent)
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, " " + e.message, Toast.LENGTH_SHORT).show()
            }
        }
        /** Speech Recognition End **/

        val editItemSwipeHandler = object : SwipeToEditCallback(this) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val rvAdapter = binding!!.rvTodoList.adapter as MainActivityAdapter
                val itemToEdit = rvAdapter.findSwipedItem(viewHolder.adapterPosition)
                updateTask(itemToEdit.id)
            }
        }

        val editItemTouchHandler = ItemTouchHelper(editItemSwipeHandler)
        editItemTouchHandler.attachToRecyclerView(binding?.rvTodoList)

        val deleteSwipeHandler = object : SwipeToDeleteCallback(this) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val rvAdapter = binding!!.rvTodoList.adapter as MainActivityAdapter
                val itemToDelete = rvAdapter.findSwipedItem(viewHolder.adapterPosition)
                oldItem = itemToDelete
                oldList = list!!
                deleteFromDatabase(itemToDelete, dao)
            }
        }

        val deleteItemTouchHandler = ItemTouchHelper(deleteSwipeHandler)
        deleteItemTouchHandler.attachToRecyclerView(binding?.rvTodoList)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.app_bar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.miExportExcel -> requestStoragePermission { exportExcel() }
            R.id.miExit -> finish()
        }
        return true
    }

    private fun requestStoragePermission(onPermissionGranted: () -> Unit) {
        Dexter.withContext(this)
            .withPermissions(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    if (report.areAllPermissionsGranted()) {
                        onPermissionGranted.invoke()
                    } else {
                        Toast.makeText(this@MainActivity, "Permission Denied", Toast.LENGTH_SHORT).show()
                        showRationalDialogForPermissions()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>,
                    token: PermissionToken
                ) {
                    token.continuePermissionRequest()
                }
            }).check()
    }

    private fun exportExcel() {
        /** creating workbook and sheet **/
        val workbook = HSSFWorkbook()
        val sheet = workbook.createSheet("Tasks")

        /** writing the data to the sheet **/
        var row = sheet.createRow(0)
        row.createCell(0).setCellValue("id")
        row.createCell(1).setCellValue("title")
        row.createCell(2).setCellValue("completed")
        row.createCell(3).setCellValue("timestamp")

        for (i in list!!.indices) {
            val task = list!![i]
            row = sheet.createRow(i + 1)
            row.createCell(0).setCellValue(task.id.toString())
            row.createCell(1).setCellValue(task.title)
            row.createCell(2).setCellValue(task.completed.toString())
            row.createCell(3).setCellValue(task.timestamp)
        }

        /** Saving the data to a file **/
        //val exportDir = File(Environment.getStorageDirectory(), "TasksAppData")
        val exportDir = this.getExternalFilesDir(null)


        Log.e("Storage", exportDir!!.absolutePath)
        Log.e("List", list.toString())


        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }

        val file = File(exportDir, "TasksData.xls")
        Log.e("Storage", file.toString())
        try {
            val outputStream = FileOutputStream(file)
            workbook.write(outputStream)
            workbook.close()
            outputStream.close()
            Toast.makeText(applicationContext, "Exported to ${exportDir!!.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(applicationContext, "There was an error: $e", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun showRationalDialogForPermissions() {
        AlertDialog.Builder(this).setMessage("Permissions denied for this app")
            .setPositiveButton("GO TO SETTINGS") { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }.setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    private fun getTodos() {
        lifecycleScope.launch {
            dao.fetchAllTasks().collect {
                list = ArrayList(it)
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm")

                /** sorting the elements **/
                list!!.sortByDescending { sdf.parse(it.timestamp)?.time }
                list!!.sortBy { it.completed }
                rvSetup(list!!)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun addTodo() {
        val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")
        val currentDate = LocalDateTime.now().format(formatter)
        val todoText = binding!!.etTodo.text.toString()

        if (todoText.isNotEmpty()) {
            lifecycleScope.launch {
                dao.insert(TaskEntity(title = todoText, timestamp = currentDate))
//                Toast.makeText(applicationContext, "Record saved", Toast.LENGTH_LONG).show()
                binding!!.etTodo.text?.clear()
            }
        } else {
            Toast.makeText(
                applicationContext,
                "You need to enter data to save", Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun rvSetup(todos: ArrayList<TaskEntity>) {
        /** setting up the recycler view **/
        val adapter = MainActivityAdapter(todos)
        binding!!.rvTodoList.layoutManager = LinearLayoutManager(this)
        binding!!.rvTodoList.adapter = adapter

        if (todos.isNotEmpty()) {
            binding!!.rvTodoList.visibility = View.VISIBLE
        } else {
            binding!!.rvTodoList.visibility = View.INVISIBLE
        }

        adapter.setOnClick(object : MainActivityAdapter.OnClickList {
            override fun onClick(position: Int, model: TaskEntity) {
                val indexOfClickedItem = list!!.indexOf(model)
                println(list!![indexOfClickedItem])
                updateTaskCompleted(list!![indexOfClickedItem])
            }
        })
    }

    private fun updateTaskCompleted(position: TaskEntity) {
        lifecycleScope.launch {
            dao.update(position)
            if (position.completed) {
                Toast.makeText(
                    applicationContext, "Task marked Complete.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    applicationContext, "Task marked Incomplete.",
                    Toast.LENGTH_SHORT
                ).show()
                dao.fetchAllTasks()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateTask(id: Int) {
        /** START of code related to the update dialog**/
        val updateDialog = Dialog(this, R.style.Theme_Dialog)
        //avoiding that clicking outside will not close the dialog or update data
        updateDialog.setCancelable(false)
        val binding = DialogUpdateBinding.inflate(layoutInflater)
        updateDialog.setContentView(binding.root)
        updateDialog.show()

        /** START of interaction with the db
         * reading clicked element by Id and putting the original text to the fields**/
        lifecycleScope.launch {
            dao.fetchTaskById(id).collect {
                binding.etUpdateNote.setText(it.title)
            }
        }

        /** UPDATE execution**/
        binding.tvUpdate.setOnClickListener {
            val task = binding.etUpdateNote.text.toString()
            val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")
            val currentTimeStamp = LocalDateTime.now().format(formatter)
            val completed = false

            if (task.isNotEmpty()) {
                lifecycleScope.launch {
                    dao.update(TaskEntity(id, task, completed, currentTimeStamp))
                    updateDialog.dismiss()
                }
            } else {
                Toast.makeText(
                    applicationContext, "New note cannot be blank.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        /** Dismiss the dialog on cancel button**/
        binding.tvCancel.setOnClickListener {
            updateDialog.dismiss()
            /** Resetting the swipped view **/
            getTodos()
        }
    }

    private fun deleteFromDatabase(position: TaskEntity, taskDao: TaskDao) {
        lifecycleScope.launch {
            taskDao.delete(position)
            showSnackbar()
        }
    }

    private fun undoDelete(){
        lifecycleScope.launch {
            dao.insert(oldItem)
        }
    }

    private fun showSnackbar(){
        val view = findViewById<View>(R.id.mainLayout)
        val snackbar = Snackbar.make(view, "Task deleted!", Snackbar.LENGTH_LONG)
        snackbar.setAction("Undo"){
            undoDelete()
        }
            .setActionTextColor(ContextCompat.getColor(this, R.color.red))
            .setTextColor(ContextCompat.getColor(this, R.color.white))
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}