package com.defianttech.convertme

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import kotlinx.android.synthetic.main.custom_units_add_activity.*

/*
 * Copyright (c) 2020 Dmitry Brant
 */
class CustomUnitsAddActivity : AppCompatActivity() {
    private var categories: Array<UnitCollection> = UnitCollection.getInstance(this)
    private var allCategoryNames: Array<String> = UnitCollection.getAllCategoryNames(this)
    private val textWatcher: UnitTextWatcher = UnitTextWatcher()
    private var editUnit: CustomUnits.CustomUnit? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.custom_units_add_activity)
        setSupportActionBar(toolbar)

        val editUnitId = intent.getIntExtra(ConvertActivity.INTENT_EXTRA_UNIT_ID, 0)
        if (editUnitId != 0) {
            editUnit = UnitCollection.getCustomUnits(this).units.first { unit -> unit.id == editUnitId }
        }

        supportActionBar?.setTitle(if (isEditing()) R.string.edit_unit else R.string.add_new_unit)

        val categoryAdapter = ArrayAdapter(this, R.layout.unit_categoryitem, allCategoryNames)

        unit_category_spinner.adapter = categoryAdapter
        unit_category_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>, view: View, index: Int, l: Long) {
                val currentCategory = UnitCollection.collectionIndexByName(categories, allCategoryNames[index])
                unit_base_spinner.adapter = ArrayAdapter(this@CustomUnitsAddActivity, R.layout.unit_categoryitem, categories[currentCategory].items)

                if (isEditing()) {
                    val defaultIndex = categories[editUnit!!.categoryId].items.indexOfFirst { unit -> unit.id == editUnit!!.baseUnitId }
                    if (defaultIndex >= 0) {
                        unit_base_spinner.setSelection(defaultIndex)
                    }
                } else {
                    // find the default base unit in this collection
                    val defaultIndex = categories[currentCategory].items.indexOfFirst { unit -> unit.multiplier == 1.0 }
                    if (defaultIndex >= 0) {
                        unit_base_spinner.setSelection(defaultIndex)
                    }
                }
                updatePreview()
            }
            override fun onNothingSelected(adapterView: AdapterView<*>) {}
        }

        unit_base_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>, view: View, index: Int, l: Long) {
                updatePreview()
            }
            override fun onNothingSelected(adapterView: AdapterView<*>) {}
        }

        unit_name_text.addTextChangedListener(textWatcher)
        unit_multiplier_text.addTextChangedListener(textWatcher)

        invert_button.setOnClickListener {
            val multiplier = unit_multiplier_text.text.toString().toDoubleOrNull()
            if (multiplier != null) {
                if (multiplier == 0.0) {
                    AlertDialog.Builder(this@CustomUnitsAddActivity)
                            .setMessage(R.string.nice_try)
                            .setPositiveButton(android.R.string.ok, null)
                            .create()
                            .show()
                } else {
                    unit_multiplier_text.setText((1 / multiplier).toString())
                }
            }
        }

        add_button.setText(if (isEditing()) R.string.done_button else R.string.add_button)
        add_button.setOnClickListener {
            if (isEditing()) {
                commitEditUnit()
            } else {
                addNewUnit()
            }
        }

        if (isEditing()) {
            unit_category_spinner.setSelection(editUnit!!.categoryId)
            unit_category_spinner.isEnabled = false

            unit_name_text.setText(editUnit!!.name)
            unit_multiplier_text.setText((1.0 / editUnit!!.multiplier).toString())
        }
    }

    public override fun onDestroy() {
        super.onDestroy()
        unit_name_text.removeTextChangedListener(textWatcher)
        unit_multiplier_text.removeTextChangedListener(textWatcher)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return false
    }

    private fun isEditing(): Boolean {
        return editUnit != null
    }

    private fun updatePreview() {
        if (unit_category_spinner.selectedItemPosition == -1 || unit_base_spinner.selectedItemPosition == -1) {
            return
        }
        val multiplier = unit_multiplier_text.text.toString().toDoubleOrNull()
        val currentCategory = UnitCollection.collectionIndexByName(categories, allCategoryNames[unit_category_spinner.selectedItemPosition])
        val baseUnitId = categories[currentCategory].items[unit_base_spinner.selectedItemPosition].id
        val baseUnit = categories[currentCategory].items.first { u -> u.id == baseUnitId }
        if (unit_name_text.text.isNullOrEmpty() || multiplier == null || multiplier == 0.0 || baseUnit == null) {
            unit_preview_label.visibility = View.GONE
            unit_preview_text.visibility = View.GONE
            return
        }
        unit_preview_label.visibility = View.VISIBLE
        unit_preview_text.visibility = View.VISIBLE
        unit_preview_text.text = "1 " +  unit_name_text.text + " = " + multiplier + " " + baseUnit.name + "\n" +
                "1 " + baseUnit.name + " = " + (1.0 / multiplier) + " " + unit_name_text.text
    }

    private fun addNewUnit() {
        val currentCategory = UnitCollection.collectionIndexByName(categories, allCategoryNames[unit_category_spinner.selectedItemPosition])
        val baseUnitId = categories[currentCategory].items[unit_base_spinner.selectedItemPosition].id
        val multiplier = unit_multiplier_text.text.toString().toDoubleOrNull()
        if (multiplier == null || multiplier == 0.0) {
            unit_multiplier_input.error = getString(R.string.custom_unit_multiplier_invalid)
            return
        }
        if (unit_name_text.text.isNullOrEmpty()) {
            unit_name_input.error = getString(R.string.custom_unit_name_invalid)
            return
        }
        UnitCollection.addCustomUnit(this, currentCategory, baseUnitId, 1.0 / multiplier, unit_name_text.text.toString())
        setResult(ConvertActivity.RESULT_CODE_CUSTOM_UNITS_CHANGED)
        finish()
    }

    private fun commitEditUnit() {
        val currentCategory = editUnit!!.categoryId
        val baseUnitId = categories[currentCategory].items[unit_base_spinner.selectedItemPosition].id
        val multiplier = unit_multiplier_text.text.toString().toDoubleOrNull()
        if (multiplier == null || multiplier == 0.0) {
            unit_multiplier_input.error = getString(R.string.custom_unit_multiplier_invalid)
            return
        }
        if (unit_name_text.text.isNullOrEmpty()) {
            unit_name_input.error = getString(R.string.custom_unit_name_invalid)
            return
        }
        UnitCollection.editCustomUnit(this, editUnit!!.id, baseUnitId, 1.0 / multiplier, unit_name_text.text.toString())
        setResult(ConvertActivity.RESULT_CODE_CUSTOM_UNITS_CHANGED)
        finish()
    }

    private inner class UnitTextWatcher : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            updatePreview()
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }
    }
}

