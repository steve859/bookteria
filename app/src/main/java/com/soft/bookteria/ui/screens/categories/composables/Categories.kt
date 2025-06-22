package com.soft.bookteria.ui.screens.categories.composables

import com.soft.bookteria.R

sealed class Categories(val category: String, val nameResource: Int) {
    data object Animal : Categories("animal", R.string.category_animal)
    data object Children : Categories("children", R.string.category_children)
    data object Classics : Categories("classics", R.string.category_classics)
    data object Countries : Categories("countries", R.string.category_countries)
    data object Crime : Categories("crime", R.string.category_crime)
    data object Education : Categories("education", R.string.category_education)
    data object Fiction : Categories("fiction", R.string.category_fiction)
    data object Geography : Categories("geography", R.string.category_geography)
    data object History : Categories("history", R.string.category_history)
    data object Literature : Categories("literature", R.string.category_literature)
    data object Law : Categories("law", R.string.category_law)
    data object Music : Categories("music", R.string.category_music)
    data object Periodicals : Categories("periodicals", R.string.category_periodicals)
    data object Psychology : Categories("psychology", R.string.category_psychology)
    data object Philosophy : Categories("philosophy", R.string.category_philosophy)
    data object Religion : Categories("religion", R.string.category_religion)
    data object Romance : Categories("romance", R.string.category_romance)
    data object Science : Categories("science", R.string.category_science)
    
    companion object {
        val allCategories = listOf(
            Animal,
            Children,
            Classics,
            Countries,
            Crime,
            Education,
            Fiction,
            Geography,
            History,
            Literature,
            Law,
            Music,
            Periodicals,
            Psychology,
            Philosophy,
            Religion,
            Romance,
            Science
        )
        
        fun fromString(categoryString: String): Categories? {
            return allCategories.find { it.category == categoryString }
        }
        
        fun getCategoryNames(): List<String> {
            return allCategories.map { it.category }
        }
    }
}