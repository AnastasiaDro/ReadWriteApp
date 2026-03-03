package com.cerebus.readwrite.di.feature

import com.cerebus.data.student.data.StudentRepositoryImpl
import com.cerebus.data.student.data.storage.StudentStorage
import com.cerebus.data.student.data.storage.StudentStorageImpl
import com.cerebus.data.student.domain.repositories.StudentRepository
import com.cerebus.readwrite.view.AppStartViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val studentsModule = module {
    single<StudentRepository> {
        StudentRepositoryImpl(storage = get())
    }

    single<StudentStorage> {
        StudentStorageImpl(dao = get())
    }

    viewModelOf(::AppStartViewModel)
}
