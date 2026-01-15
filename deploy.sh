#!/bin/bash

# Comprobar si se pasaron los argumentos necesarios
if [ -z "$1" ] || [ -z "$2" ]; then
  echo "❌ Error: Faltan argumentos."
  echo "Uso: ./deploy.sh <version> '<mensaje_del_commit>'"
  echo "Ejemplo: ./deploy.sh v1.12.0 'Arreglados los permisos de escritura'"
  exit 1
fi

VERSION=$1
MESSAGE=$2

echo "🚀 Iniciando despliegue de la versión $VERSION..."

# 1. Guardar y subir código a main
git add .
git commit -m "$MESSAGE"
git push origin main

# 2. Limpiar tags antiguos (por si acaso quieres re-subir la misma versión)
echo "🧹 Limpiando tags previos..."
git tag -d $VERSION 2>/dev/null
git push --delete origin $VERSION 2>/dev/null

# 3. Crear y subir el nuevo tag
echo "🏷️ Creando tag $VERSION..."
git tag $VERSION
git push origin $VERSION

echo "✅ ¡Todo listo! El robot de GitHub Actions debería estar trabajando ahora."