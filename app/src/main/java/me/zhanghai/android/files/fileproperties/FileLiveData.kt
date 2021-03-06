/*
 * Copyright (c) 2019 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.fileproperties

import android.os.AsyncTask
import java8.nio.file.Path
import me.zhanghai.android.files.file.FileItem
import me.zhanghai.android.files.file.loadFileItem
import me.zhanghai.android.files.filelist.PathObserver
import me.zhanghai.android.files.util.CloseableLiveData

class FileLiveData private constructor(
    private val path: Path,
    file: FileItem?
) : CloseableLiveData<FileData>() {
    private val observer: PathObserver

    @Volatile
    private var changedWhileInactive = false

    constructor(path: Path) : this(path, null)

    constructor(file: FileItem) : this(file.path, file)

    init {
        if (file != null) {
            value = FileData.ofSuccess(file)
        } else {
            loadValue()
        }
        observer = PathObserver(path) { onChangeObserved() }
    }

    fun loadValue() {
        value = FileData.ofLoading()
        AsyncTask.THREAD_POOL_EXECUTOR.execute {
            val value = try {
                val file = path.loadFileItem()
                FileData.ofSuccess(file)
            } catch (e: Exception) {
                FileData.ofError(e)
            }
            postValue(value)
        }
    }

    private fun onChangeObserved() {
        if (hasActiveObservers()) {
            loadValue()
        } else {
            changedWhileInactive = true
        }
    }

    override fun onActive() {
        if (changedWhileInactive) {
            loadValue()
            changedWhileInactive = false
        }
    }

    override fun close() {
        observer.close()
    }
}
