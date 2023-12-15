package jp.techacademy.motoyoshi.taskapp

import android.R
import android.app.AlarmManager
import android.app.AlarmManager.AlarmClockInfo
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.ext.query
import jp.techacademy.motoyoshi.taskapp.databinding.ActivityInputBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*


class InputActivity : AppCompatActivity() {
    private lateinit var binding: ActivityInputBinding

    private lateinit var realm: Realm
    private lateinit var task: Task
    private var calendar = Calendar.getInstance(Locale.JAPANESE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInputBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // アクションバーの設定
        setSupportActionBar(binding.toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }

        // ボタンのイベントリスナーの設定
        binding.content.dateButton.setOnClickListener(dateClickListener)
        binding.content.timeButton.setOnClickListener(timeClickListener)
        binding.content.doneButton.setOnClickListener(doneClickListener)
        binding.content.categoryCreateButton.setOnClickListener(createClickListener)

        // EXTRA_TASKからTaskのidを取得
        val intent = intent
        val taskId = intent.getIntExtra(EXTRA_TASK, -1)

        // Realmデータベースとの接続を開く
        val config = RealmConfiguration.Builder(setOf(Task::class, Category::class)).build()
        realm = Realm.open(config)
        // タスクを取得または初期化
        initTask(taskId)
        // スピナーをセット
        setupCategorySpinner()

    }

    override fun onDestroy() {
        super.onDestroy()

        // Realmデータベースとの接続を閉じる
        realm.close()
    }

        /**
         * 作成ボタン
         */
        private val createClickListener = View.OnClickListener {
            //画面遷移のみ、現在の画面のライフサイクルはPauseになったままでいい
            //カテゴリ―作成時に戻るため
            //画面遷移の実行
            val intent = Intent(this, CategoryInput::class.java)
            startActivity(intent)
        }

        /**
         * 日付選択ボタン
         */
        private val dateClickListener = View.OnClickListener {
            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    setDateTimeButtonText()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }

        /**
         * 時刻選択ボタン
         */
        private val timeClickListener = View.OnClickListener {
            val timePickerDialog = TimePickerDialog(
                this,
                { _, hour, minute ->
                    calendar.set(Calendar.HOUR_OF_DAY, hour)
                    calendar.set(Calendar.MINUTE, minute)
                    setDateTimeButtonText()
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true
            )
            timePickerDialog.show()
        }

        /**
         * 決定ボタン
         */
        private val doneClickListener = View.OnClickListener {
            CoroutineScope(Dispatchers.Default).launch {
                addTask()
                finish()
            }
        }

        /**
         * タスクを取得または初期化
         */
        private fun initTask(taskId: Int) {
            // 引数のtaskIdに合致するタスクを検索
            val findTask = realm.query<Task>("id==$taskId").first().find()

            if (findTask == null) {
                // 新規作成の場合
                task = Task()
                task.id = -1

                // 日付の初期値を1日後に設定
                calendar.add(Calendar.DAY_OF_MONTH, 1)

            } else {
                // 更新の場合
                task = findTask

                // taskの日時をcalendarに反映
                val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.JAPANESE)
                calendar.time = simpleDateFormat.parse(task.date) as Date

                // taskの値を画面項目に反映
                binding.content.titleEditText.setText(task.title)
                binding.content.contentEditText.setText(task.contents)

                // spinnerの
                val selectedBValue = task.category?.category_content
                val selectedIndex = getUniqueCategoriesFromRealm().indexOf(selectedBValue)
                if (selectedIndex >= 0) {
                    binding.content.inputCategorySpinner.setSelection(selectedIndex)
                }
            }

            // 日付と時刻のボタンの表示を設定
            setDateTimeButtonText()

        }

        /**
         * タスクの登録または更新を行う カテゴリー情報の追加
         */
        private suspend fun addTask() {
            // 日付型オブジェクトを文字列に変換用
            val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.JAPANESE)

            // 登録（更新）する値を取得
            val title = binding.content.titleEditText.text.toString()
            val content = binding.content.contentEditText.text.toString()
            val date = simpleDateFormat.format(calendar.time)
            val categoryContent = binding.content.inputCategorySpinner.selectedItem as String
            val categoryId = realm.query<Category>("category_content == $0", categoryContent).first().find()?.category_id ?: return
            val taskId = task.id // 既存のタスクのIDを取得

            realm.writeBlocking {
                // 新規タスクの場合
                if (taskId == -1) {
                    // 新しいタスクを作成
                    val newTask = Task().apply {
                        this.id = (realm.query<Task>().max("id", Int::class).find() ?: -1) + 1
                        this.title = title
                        this.contents = content
                        this.date = date
                        // カテゴリの設定
                        this.category = query<Category>("category_id == $0", categoryId).first().find()
                    }
                    copyToRealm(newTask)
                } else {
                    // 既存のタスクを更新
                    val updateTask = query<Task>("id == $0", taskId).first().find()
                    val updateCategory = query<Category>("category_id == $0", categoryId).first().find()

                    updateTask?.apply {
                        this.title = title
                        this.contents = content
                        this.date = date
                        this.category = updateCategory // 再取得したカテゴリを設定
                    }
                }
            }


            // タスクの日時にアラームを設定
            val intent = Intent(applicationContext, TaskAlarmReceiver::class.java)
            intent.putExtra(EXTRA_TASK, task.id)
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                task.id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            alarmManager.setAlarmClock(AlarmClockInfo(calendar.timeInMillis, null), pendingIntent)
        }

    /**
         * 日付と時刻のボタンの表示を設定する
         */
        private fun setDateTimeButtonText() {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.JAPANESE)
            binding.content.dateButton.text = dateFormat.format(calendar.time)

            val timeFormat = SimpleDateFormat("HH:mm", Locale.JAPANESE)
            binding.content.timeButton.text = timeFormat.format(calendar.time)

        }

        // Realmからユニークなカテゴリーデータを取得する関数
        private fun getUniqueCategoriesFromRealm(): List<String?> {

            // カテゴリーデータの取得
            val categories = realm.query<Category>()
                .find()
                .map { category -> category.category_content }
                .distinct()

            return categories
        }

        // スピナーを設定する関数
        private fun setupCategorySpinner() {
            val categories = getUniqueCategoriesFromRealm()
            val adapter = ArrayAdapter(this, R.layout.simple_spinner_item, categories)
            adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
            binding.content.inputCategorySpinner.adapter = adapter
        }
}






