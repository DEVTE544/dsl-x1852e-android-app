package com.example.d_linkmobilymanagement.ui.screens.auth
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.d_linkmobilymanagement.R
import com.example.d_linkmobilymanagement.data.model.PreLoginInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    defaultRouterIp: String,
    defaultUsername: String,
    defaultPassword: String = "", // 🟢 new: receive saved password
    isLoading: Boolean,
    errorMessageRes: Int?,
    preLoginInfo: PreLoginInfo?, //  receive router information
    onIpChanged: (String) -> Unit,
    // 🟢 new: pass remember me state as Boolean in function
    onLogin: (String, String, String, Boolean) -> Unit 
) {
    var ip by remember { mutableStateOf(defaultRouterIp) }
    
    //  username will contain the "actual value" (example: telecomadmin)
    var username by remember { mutableStateOf(defaultUsername) }
    var password by remember { mutableStateOf(defaultPassword) } // set default
    
    // 🟢 new: remember me state (make it True by default if there's saved password)
    var rememberMe by remember { mutableStateOf(defaultPassword.isNotEmpty()) }
    
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(ip) {
        if (ip.isNotBlank() && ip.length > 7) {
            kotlinx.coroutines.delay(500)
            onIpChanged(ip)
        }
    }

    LaunchedEffect(defaultRouterIp, defaultUsername, defaultPassword) {
        if (ip.isBlank()) ip = defaultRouterIp
        if (username.isBlank()) username = defaultUsername
        if (password.isBlank()) password = defaultPassword
    }

    //  username is now the actual display name (SuperAdmin) that router accepts

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.Router,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.login_to_router),
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(24.dp))

        if (preLoginInfo != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = stringResource(R.string.router_found), 
                        style = MaterialTheme.typography.labelSmall, 
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${stringResource(R.string.router_model)}: ${preLoginInfo.modelName}", 
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${stringResource(R.string.router_firmware)}: ${preLoginInfo.firmwareVersion}", 
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            //  update default username to be first available option (automatically)
            LaunchedEffect(preLoginInfo) {
                if (preLoginInfo.availableUsers.isNotEmpty() && !preLoginInfo.availableUsers.contains(username)) {
                    username = preLoginInfo.availableUsers.first()
                }
            }
        }

        if (errorMessageRes != null) {
            Text(
                text = stringResource(errorMessageRes),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        OutlinedTextField(
            value = ip,
            onValueChange = { ip = it },
            label = { Text(stringResource(R.string.router_ip)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            shape = MaterialTheme.shapes.medium
        )
        Spacer(Modifier.height(12.dp))

        //  simple dropdown (shows and sends SuperAdmin which router accepts)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { if (!isLoading) expanded = it }
        ) {
            OutlinedTextField(
                value = username, // displays and sends SuperAdmin
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.username)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                enabled = !isLoading,
                shape = MaterialTheme.shapes.medium
            )
            
            val usersList = preLoginInfo?.availableUsers ?: listOf(username)
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                usersList.forEach { userStr ->
                    DropdownMenuItem(
                        text = { Text(userStr) },
                        onClick = {
                            username = userStr 
                            expanded = false
                        }
                    )
                }
            }
        }
        
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.password)) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            shape = MaterialTheme.shapes.medium
        )
        Spacer(Modifier.height(8.dp))
        
        // 🟢 new: remember me checkbox UI
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { rememberMe = !rememberMe },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = rememberMe,
                onCheckedChange = { rememberMe = it },
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
            )
            Text(
                text = stringResource(R.string.remember_me),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(Modifier.height(24.dp))

        Button(
            // 🟢 send rememberMe value
            onClick = { onLogin(ip, username, password, rememberMe) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp),
            enabled = !isLoading,
            shape = MaterialTheme.shapes.medium
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(stringResource(R.string.login), fontSize = 18.sp)
            }
        }
    }
}
