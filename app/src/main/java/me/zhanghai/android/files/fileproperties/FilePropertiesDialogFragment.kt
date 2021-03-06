/*
 * Copyright (c) 2018 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.fileproperties

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.Fragment
import kotlinx.android.parcel.Parcelize
import me.zhanghai.android.files.R
import me.zhanghai.android.files.compat.AlertDialogBuilderCompat
import me.zhanghai.android.files.databinding.FilePropertiesDialogBinding
import me.zhanghai.android.files.file.FileItem
import me.zhanghai.android.files.filelist.name
import me.zhanghai.android.files.fileproperties.basic.FilePropertiesBasicTabFragment
import me.zhanghai.android.files.fileproperties.permissions.FilePropertiesPermissionsTabFragment
import me.zhanghai.android.files.ui.TabFragmentPagerAdapter
import me.zhanghai.android.files.util.ParcelableArgs
import me.zhanghai.android.files.util.args
import me.zhanghai.android.files.util.layoutInflater
import me.zhanghai.android.files.util.putArgs
import me.zhanghai.android.files.util.show
import me.zhanghai.android.files.util.viewModels

class FilePropertiesDialogFragment : AppCompatDialogFragment() {
    private val args by args<Args>()

    private val viewModel by viewModels { { FilePropertiesViewModel(args.file) } }

    private lateinit var binding: FilePropertiesDialogBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        AlertDialogBuilderCompat.create(requireContext(), theme)
            .setTitle(getString(R.string.file_properties_title_format, args.file.name))
            .apply {
                binding = FilePropertiesDialogBinding.inflate(context.layoutInflater)
                setView(binding.root)
            }
            .setPositiveButton(android.R.string.ok, null)
            .create()

    // HACK: Work around child FragmentManager requiring a view.
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = binding.root

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // Initialize our view model before child fragments are created.
        viewModel.fileLiveData
        val tabs = mutableListOf<Pair<Int, () -> Fragment>>()
            .apply {
                add(R.string.file_properties_basic to { FilePropertiesBasicTabFragment() })
                if (FilePropertiesPermissionsTabFragment.isAvailable(args.file)) {
                    add(
                        R.string.file_properties_permissions
                            to { FilePropertiesPermissionsTabFragment() }
                    )
                }
            }
            .map { getString(it.first) to it.second }
            .toTypedArray()
        val tabAdapter = TabFragmentPagerAdapter(childFragmentManager, *tabs)
        binding.viewPager.offscreenPageLimit = tabAdapter.count - 1
        binding.viewPager.adapter = tabAdapter
        binding.tabLayout.setupWithViewPager(binding.viewPager)
    }

    companion object {
        fun show(file: FileItem, fragment: Fragment) {
            FilePropertiesDialogFragment().putArgs(Args(file)).show(fragment)
        }
    }

    @Parcelize
    class Args(val file: FileItem): ParcelableArgs
}
