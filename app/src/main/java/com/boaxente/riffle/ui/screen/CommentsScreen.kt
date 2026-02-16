package com.boaxente.riffle.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.boaxente.riffle.R
import com.boaxente.riffle.data.model.CommentNode
import com.boaxente.riffle.ui.viewmodel.CommentsViewModel
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsScreen(
    articleLink: String,
    articleTitle: String,
    onBack: () -> Unit,
    viewModel: CommentsViewModel = hiltViewModel()
) {
    val comments by viewModel.comments.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val replyingTo by viewModel.replyingTo.collectAsState()
    val error by viewModel.error.collectAsState()
    val loadingRepliesFor by viewModel.loadingRepliesFor.collectAsState()
    
    var commentText by remember { mutableStateOf("") }
    var showLoginPrompt by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var commentToDelete by remember { mutableStateOf<CommentNode?>(null) }
    
    val currentUser = FirebaseAuth.getInstance().currentUser
    val isLoggedIn = currentUser != null
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    
    LaunchedEffect(articleLink) {
        viewModel.loadComments(articleLink, articleTitle)
    }
    
    // Scroll al último comentario cuando se añade uno nuevo
    LaunchedEffect(comments.size) {
        if (comments.isNotEmpty()) {
            listState.animateScrollToItem(comments.size - 1)
        }
    }
    
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.comments_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.article_back))
                    }
                }
            )
        },
        bottomBar = {
            Column {
                // Indicador de respuesta
                AnimatedVisibility(visible = replyingTo != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.comments_replying_to, replyingTo?.comment?.userName ?: ""),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            IconButton(
                                onClick = { viewModel.setReplyingTo(null) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = stringResource(R.string.comments_cancel_reply),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
                
                // Campo de entrada de comentario
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text(stringResource(R.string.comments_placeholder)) },
                            maxLines = 3,
                            shape = RoundedCornerShape(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (!isLoggedIn) {
                                    showLoginPrompt = true
                                } else if (commentText.isNotBlank()) {
                                    viewModel.addComment(commentText.trim())
                                    commentText = ""
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                }
                            },
                            enabled = commentText.isNotBlank() || !isLoggedIn
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = stringResource(R.string.comments_send),
                                tint = if (commentText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            when {
                isLoading && comments.isEmpty() -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                comments.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.ChatBubbleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.comments_empty),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.comments_empty_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(comments) { commentNode ->
                            CommentTree(
                                commentNode = commentNode,
                                currentUserId = currentUser?.uid,
                                onReply = { 
                                    if (isLoggedIn) {
                                        viewModel.setReplyingTo(it)
                                    } else {
                                        showLoginPrompt = true
                                    }
                                },
                                onLike = { viewModel.likeComment(it.comment.id) },
                                onDislike = { viewModel.dislikeComment(it.comment.id) },
                                onDelete = { 
                                    commentToDelete = it
                                    showDeleteDialog = true
                                },
                                onLoadReplies = { viewModel.loadReplies(it.comment.id) },
                                isReplyLoading = loadingRepliesFor.contains(commentNode.comment.id)
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Diálogo de login requerido
    if (showLoginPrompt) {
        AlertDialog(
            onDismissRequest = { showLoginPrompt = false },
            title = { Text(stringResource(R.string.comments_login_required_title)) },
            text = { Text(stringResource(R.string.comments_login_required_text)) },
            confirmButton = {
                TextButton(onClick = { showLoginPrompt = false }) {
                    Text(stringResource(R.string.dialog_ok))
                }
            }
        )
    }
    
    // Diálogo de confirmar borrado
    if (showDeleteDialog && commentToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.comments_delete_title)) },
            text = { Text(stringResource(R.string.comments_delete_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        commentToDelete?.let { viewModel.deleteComment(it.comment.id) }
                        showDeleteDialog = false
                        commentToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.comments_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.comments_delete_cancel))
                }
            }
        )
    }
    
    // Mostrar error
    error?.let { errorMessage ->
        LaunchedEffect(errorMessage) {
            // Auto dismiss after showing
            viewModel.clearError()
        }
    }
}

@Composable
fun CommentTree(
    commentNode: CommentNode,
    currentUserId: String?,
    onReply: (CommentNode) -> Unit,
    onLike: (CommentNode) -> Unit,
    onDislike: (CommentNode) -> Unit,
    onDelete: (CommentNode) -> Unit,
    onLoadReplies: (CommentNode) -> Unit,
    isReplyLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    val lineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
    
    Column(modifier = modifier) {
        CommentItem(
            commentNode = commentNode,
            currentUserId = currentUserId,
            onReply = { onReply(commentNode) },
            onLike = { onLike(commentNode) },
            onDislike = { onDislike(commentNode) },
            onDelete = { onDelete(commentNode) }
        )
        
        // Mostrar respuestas
        if (commentNode.replies.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .padding(start = 12.dp) // Alineación inicial con el padding del padre
            ) {
                commentNode.replies.forEachIndexed { index, reply ->
                    // Padding start para dejar espacio a la línea vertical y crear la estructura de árbol
                    // También dibujamos la pequeña curva horizontal para cada item
                    val isLast = index == commentNode.replies.lastIndex
                    
                    Box(
                        modifier = Modifier
                            .padding(start = 32.dp) // 16dp (linea) + 16dp (espacio)
                            .drawBehind {
                                val lineX = -16.dp.toPx()
                                val yPos = 24.dp.toPx() // Aprox mitad de altura de header de usuario (que tiene uns 48dp aprox con padding)
                                val strokeWidth = 2.dp.toPx()
                                val radius = 16.dp.toPx()
                                
                                // Crear un único path para todo el conector
                                // Esto evita que la superposición de la línea vertical y la curva 
                                // cree un efecto de oscurecimiento (alpha blending) en la intersección
                                val path = androidx.compose.ui.graphics.Path()

                                // 1. Línea vertical
                                // Si es el último, la línea solo baja hasta el inicio de la curva
                                val verticalEnd = if (isLast) yPos - radius else size.height
                                
                                path.moveTo(lineX, 0f)
                                path.lineTo(lineX, verticalEnd)
                                
                                // 2. Curva
                                path.moveTo(lineX, yPos - radius)
                                path.quadraticTo(
                                    lineX, yPos, // Punto de control (esquina)
                                    lineX + radius, yPos // Punto final (horizontal)
                                )
                                
                                drawPath(
                                    path = path,
                                    color = lineColor,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                                )
                                
                                // Línea horizontal final para conectar con el contenido (si el radio es menor que el offset)
                                // En este caso offset es 16dp y radio es 16dp, así que llega justo a 0
                            }
                    ) {
                        CommentTree(
                            commentNode = reply,
                            currentUserId = currentUserId,
                            onReply = onReply,
                            onLike = onLike,
                            onDislike = onDislike,
                            onDelete = onDelete,
                            onLoadReplies = onLoadReplies,
                            isReplyLoading = isReplyLoading
                        )
                    }
                }
            }
        } else if (commentNode.comment.replyCount > 0) {
            val lineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            // No cargadas, mostrar botón de cargar
            // Usamos un conector similar para que visualmente parezca parte del árbol
            
            Box(
                modifier = Modifier
                    .padding(start = 44.dp, top = 8.dp) // 12 + 32
                    .drawBehind {
                            val lineX = -16.dp.toPx()
                            val yPos = 12.dp.toPx() // Centrado relativo al texto
                            val strokeWidth = 2.dp.toPx()
                            val radius = 16.dp.toPx()
                            
                            val path = androidx.compose.ui.graphics.Path()
                            
                            // MEJOR APROXIMACIÓN: Usar la misma estructura de Box con padding y drawBehind
                            // simulando ser el primer "hijo".
                            
                            path.moveTo(lineX, -12.dp.toPx()) // Desde arriba
                            path.lineTo(lineX, yPos - radius)
                            path.quadraticTo(
                                lineX, yPos,
                                lineX + radius, yPos
                            )
                            
                            drawPath(
                                path = path,
                                color = lineColor,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                            )
                    }
            ) {
                 TextButton(
                    onClick = { onLoadReplies(commentNode) },
                    enabled = !isReplyLoading
                ) {
                    if (isReplyLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = androidx.compose.ui.res.pluralStringResource(
                            id = R.plurals.replies_count,
                            count = commentNode.comment.replyCount,
                            commentNode.comment.replyCount
                        )
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CommentItem(
    commentNode: CommentNode,
    currentUserId: String?,
    onReply: () -> Unit,
    onLike: () -> Unit,
    onDislike: () -> Unit,
    onDelete: () -> Unit
) {
    val comment = commentNode.comment
    val hasLiked = currentUserId != null && comment.likedBy.contains(currentUserId)
    val hasDisliked = currentUserId != null && comment.dislikedBy.contains(currentUserId)
    val isOwner = currentUserId != null && comment.userId == currentUserId
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Header: Usuario y fecha
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    // Avatar
                    if (comment.userPhotoUrl.isNotEmpty()) {
                        AsyncImage(
                            model = comment.userPhotoUrl,
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = comment.userName.firstOrNull()?.uppercase() ?: "?",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = comment.userName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = formatCommentDate(comment.createdAt.toDate()),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Contenido del comentario
            Text(
                text = comment.text,
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Acciones: Like, Dislike, Reply
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Like
                Row(
                    modifier = Modifier.clickable { onLike() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (hasLiked) Icons.Default.ThumbUp else Icons.Default.ThumbUpOffAlt,
                        contentDescription = stringResource(R.string.comments_like),
                        modifier = Modifier.size(18.dp),
                        tint = if (hasLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (comment.likes > 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = comment.likes.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (hasLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Dislike
                Row(
                    modifier = Modifier.clickable { onDislike() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (hasDisliked) Icons.Default.ThumbDown else Icons.Default.ThumbDownOffAlt,
                        contentDescription = stringResource(R.string.comments_dislike),
                        modifier = Modifier.size(18.dp),
                        tint = if (hasDisliked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (comment.dislikes > 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = comment.dislikes.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (hasDisliked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Reply
                Row(
                    modifier = Modifier.clickable { onReply() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Reply,
                        contentDescription = stringResource(R.string.comments_reply),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.comments_reply),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Opción de borrado para el dueño
                if (isOwner) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.comments_delete),
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun formatCommentDate(date: Date): String {
    val now = Calendar.getInstance()
    val commentDate = Calendar.getInstance().apply { time = date }
    
    return when {
        // Hoy
        now.get(Calendar.YEAR) == commentDate.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == commentDate.get(Calendar.DAY_OF_YEAR) -> {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
        }
        // Ayer
        now.get(Calendar.YEAR) == commentDate.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) - commentDate.get(Calendar.DAY_OF_YEAR) == 1 -> {
            "Ayer ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)}"
        }
        // Este año
        now.get(Calendar.YEAR) == commentDate.get(Calendar.YEAR) -> {
            SimpleDateFormat("d MMM HH:mm", Locale.getDefault()).format(date)
        }
        // Otro año
        else -> {
            SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(date)
        }
    }
}
