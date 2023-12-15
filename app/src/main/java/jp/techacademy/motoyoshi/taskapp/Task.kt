package jp.techacademy.motoyoshi.taskapp

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import java.io.Serializable

open class Task : RealmObject, Serializable {

    // idをプライマリーキーとして設定
    @PrimaryKey
    var id = 0

    var title = "" // タイトル
    var contents = "" // 内容
    var date = "" // 日時
    var category: Category? = null //カテゴリーはCategory.ktで管理、それとの紐づけを考える
}
