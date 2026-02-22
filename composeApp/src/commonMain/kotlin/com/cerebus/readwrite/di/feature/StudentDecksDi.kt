package com.cerebus.readwrite.di.feature

import com.cerebus.data.studentdeck.data.StudentDeckRepositoryImpl
import com.cerebus.data.studentdeck.data.storage.StudentDeckStorage
import com.cerebus.data.studentdeck.data.storage.StudentDeckStorageImpl
import com.cerebus.data.studentdeck.domain.repositories.StudentDeckRepository
import org.koin.dsl.module

val studentDecksModule = module {
    single<StudentDeckRepository> {
        StudentDeckRepositoryImpl(storage = get())
    }

    single<StudentDeckStorage> {
        StudentDeckStorageImpl(dao = get())
    }
}
