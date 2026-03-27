package com.cerebus.readwrite.di.feature

import com.cerebus.readwrite.view.ActiveStudentViewModel
import com.cerebus.readwrite.view.ChangeStudentViewModel
import com.cerebus.readwrite.view.CreateStudentViewModel
import com.cerebus.readwrite.view.StarterStudentService
import com.cerebus.readwrite.view.StarterStudentServiceImpl
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val studentFeatureModule = module {
    single<StarterStudentService> {
        StarterStudentServiceImpl(
            studentRepository = get(),
            preferencesRepository = get(),
            starterDeckInstaller = get(),
        )
    }
    viewModelOf(::ActiveStudentViewModel)
    viewModelOf(::ChangeStudentViewModel)
    viewModelOf(::CreateStudentViewModel)
}
