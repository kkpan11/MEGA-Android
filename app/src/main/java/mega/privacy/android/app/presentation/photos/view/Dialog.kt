package mega.privacy.android.app.presentation.photos.view

import mega.privacy.android.shared.resources.R as sharedR
import android.app.Activity
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import mega.privacy.android.app.R

fun Fragment.showSortByDialog(
    context: Activity,
    items: List<String> = listOf(
        getString(R.string.sortby_date_newest),
        getString(R.string.sortby_date_oldest),
    ),
    checkedItem: Int,
    onClickListener: (DialogInterface, Int) -> Unit,
    onDismissListener: (DialogInterface) -> Unit = {},
) {
    val sortDialog: AlertDialog
    val dialogBuilder = MaterialAlertDialogBuilder(context)

    dialogBuilder.setSingleChoiceItems(items.toTypedArray(), checkedItem) { dialog, i ->
        onClickListener(dialog, i)
        dialog.dismiss()
    }

    dialogBuilder.setNegativeButton(context.getString(sharedR.string.general_dialog_cancel_button)) {
            dialog: DialogInterface,
            _,
        ->
        dialog.dismiss()
    }

    sortDialog = dialogBuilder.create()
    sortDialog.setTitle(R.string.action_sort_by)
    sortDialog.show()
    sortDialog.setOnDismissListener {
        onDismissListener(it)
    }
}
