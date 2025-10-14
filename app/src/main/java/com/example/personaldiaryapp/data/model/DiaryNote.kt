package com.example.personaldiaryapp.data.model

data class DiaryNote(
	val id: Long? = null,
	val title: String,
	val content: String,
	val createdAt: Long
)
