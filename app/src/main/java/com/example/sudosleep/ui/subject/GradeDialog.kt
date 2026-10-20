package com.example.sudosleep.ui.subject

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun AddGradeDialog(
    subjectName: String,
    onDismissRequest: () -> Unit,
    onAddGrade: (value: Double, weight: Double) -> Unit
) {
    var gradeValueText by remember { mutableStateOf("") }
    var gradeWeightText by remember { mutableStateOf("1.0") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = "Add Grade to $subjectName",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = gradeValueText,
                    onValueChange = {
                        gradeValueText = it
                        errorMsg = null
                    },
                    label = { Text("Grade Value (1.0 - 10.0)") },
                    placeholder = { Text("e.g. 9.5") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = gradeWeightText,
                    onValueChange = {
                        gradeWeightText = it
                        errorMsg = null
                    },
                    label = { Text("Weight (e.g. 1.0 or 2.0)") },
                    placeholder = { Text("1.0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (errorMsg != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMsg!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val value = gradeValueText.toDoubleOrNull()
                    val weight = gradeWeightText.toDoubleOrNull() ?: 1.0
                    if (value == null || value < 0.0 || value > 10.0) {
                        errorMsg = "Please enter a valid grade between 0 and 10"
                        return@Button
                    }
                    onAddGrade(value, weight)
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Add Grade")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismissRequest,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
