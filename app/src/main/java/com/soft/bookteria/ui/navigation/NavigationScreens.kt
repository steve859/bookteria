package com.soft.bookteria.ui.navigation


const val BOOK_ID_ARG_KEY = "bookId"
const val LIBRARY_OBJECT_ID_ARG_KEY = "libraryObjectId"
const val CATEGORY_DETAIL_ARG_KEY = "category"

sealed class NavigationScreens(
    val route: String
){
    data object BookDetailScreen : NavigationScreens("book_detail_screen/{$BOOK_ID_ARG_KEY}"){
        fun withBookId(bookId: String): String{
            return this.route.replace("{$BOOK_ID_ARG_KEY}", bookId)
        }
    }
    
    data object CategoryScreen : NavigationScreens("category_screen/{$CATEGORY_DETAIL_ARG_KEY}"){
        fun withBookId(category: String): String{
            return this.route.replace("{$CATEGORY_DETAIL_ARG_KEY}", category)
        }
    }
    
    data object ReaderScreen : NavigationScreens("reader_screen/{$LIBRARY_OBJECT_ID_ARG_KEY}"){
        fun withBookId(id: String): String{
            return this.route.replace("{$LIBRARY_OBJECT_ID_ARG_KEY}", id)
        }
    }
    
    data object WelcomeScreen : NavigationScreens("welcome_screen")
    data object OSLScreen : NavigationScreens("osl_screen")
    data object AboutScreen : NavigationScreens("about_screen")
    
}