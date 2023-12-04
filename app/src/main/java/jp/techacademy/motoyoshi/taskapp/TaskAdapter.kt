package jp.techacademy.motoyoshi.taskapp


import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class TaskAdapter(context: Context) : BaseAdapter() {
    private val layoutInflater: LayoutInflater
    private var taskList = mutableListOf<Task>()

    init {
        this.layoutInflater = LayoutInflater.from(context)
    }

    override fun getCount(): Int {
        return taskList.size
    }

    override fun getItem(position: Int): Any {
        return taskList[position]
    }

    override fun getItemId(position: Int): Long {
        return taskList[position].id.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View =
            convertView ?: layoutInflater.inflate(R.layout.custom, parent, false)

        val textView1 = view.findViewById<TextView>(R.id.text1)
        val textView2 = view.findViewById<TextView>(R.id.text2)
        val textView3 = view.findViewById<TextView>(R.id.text3)

        textView1.text = taskList[position].title
        textView2.text = taskList[position].date
        textView3.text = taskList[position].category

        return view
    }

    fun updateTaskList(taskList: List<Task>) {
        // 一度クリアしてから新しいタスク一覧に入替
        this.taskList.clear()
        this.taskList.addAll(taskList)
        // データに変更があったことをadapterに通知
        notifyDataSetChanged()
    }
}

