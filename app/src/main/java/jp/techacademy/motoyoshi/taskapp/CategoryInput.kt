package jp.techacademy.motoyoshi.taskapp

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.ext.query
import jp.techacademy.motoyoshi.taskapp.databinding.ActivityCategoryInputBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CategoryInput : AppCompatActivity() {
    private lateinit var binding: ActivityCategoryInputBinding
    private val config = RealmConfiguration.create(schema = setOf(Category::class))
    private val realm: Realm = Realm.open(config)
    private lateinit var category: Category

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoryInputBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ボタンのイベントリスナーの設定
        binding.button1.setOnClickListener(modoruClickListener)
        binding.button2.setOnClickListener(tourokuClickListener)


    }

    private val modoruClickListener = View.OnClickListener{
        realm.close()
        finish()
    }

    private val tourokuClickListener = View.OnClickListener{

        //照合する
        //newTextとして現在の入力値を取得、これでれるむ上のカテゴリーを検索
        val newText: String = binding.editText.getText().toString()
        val findCategory = realm.query<Category>("category_content == '$newText'").find()
        //該当しない場合、これをれるむ上に登録
        if (findCategory.isEmpty()) {
            CoroutineScope(Dispatchers.Default).launch {
                category = Category()
                category.category_id =
                    (realm.query<Category>().max("category_id", Int::class).find() ?: -1) + 1
                category.category_content = newText

                realm.writeBlocking {
                    copyToRealm(category)
                }
                finish()
            }
        }else{
        //該当する場合、重複している旨表示して戻る(return)
            showAlertDialog()
        }
    }

    private fun showAlertDialog() {
        // AlertDialog.Builderクラスを使ってAlertDialogの準備をする
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle("エラー")
        alertDialogBuilder.setMessage("登録済みのカテゴリーです。")

        // 肯定ボタンに表示される文字列、押したときのリスナーを設定する
        alertDialogBuilder.setPositiveButton("OK"){dialog, which ->
        }
        // AlertDialogを作成して表示する
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }


    override fun onDestroy() {

        super.onDestroy()
        realm.close()
    }

}