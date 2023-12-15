package jp.techacademy.motoyoshi.taskapp

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.Index
import io.realm.kotlin.types.annotations.PrimaryKey
import java.io.Serializable

open class Category : RealmObject, Serializable {
    // category_idをプライマリーキーとして設定
    @PrimaryKey
    var category_id = 0

    @Index
    var category_content : String = "" //カテゴリー内容

}
