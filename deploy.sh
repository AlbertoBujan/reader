#!/bin/bash

# 1. Extraer la versión automáticamente de app/build.gradle.kts
# Buscamos la línea de versionName y extraemos el texto entre comillas
VERSION_RAW=$(grep "versionName =" app/build.gradle.kts | sed 's/.*"\(.*\)".*/\1/')

# Verificar si se encontró la versión
if [ -z "$VERSION_RAW" ]; then
  echo "❌ Error: No se pudo encontrar 'versionName' en app/build.gradle.kts"
  exit 1
fi

# Añadimos la 'v' para el Tag (ej: v1.15.0)
VERSION="v$VERSION_RAW"

echo "🔎 Versión detectada en el código: $VERSION"
echo "🚀 Iniciando despliegue automático..."

# 2. Guardar y subir código a main
git add .
git commit -m "Release $VERSION"
git push origin main

# 3. Limpiar tags antiguos (por si estás re-subiendo la misma versión)
echo "🧹 Limpiando tags previos para $VERSION..."
git tag -d $VERSION 2>/dev/null
git push --delete origin $VERSION 2>/dev/null

# 4. Crear y subir el nuevo tag
echo "🏷️ Creando tag $VERSION..."
git tag $VERSION
git push origin $VERSION

echo "✅ ¡Misión cumplida! El robot de GitHub Actions está fabricando la $VERSION."

# Launch: ./deploy.sh