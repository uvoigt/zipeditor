#!/bin/bash
# Generates p2 composite repository files from version directories in the output dir.
# Usage: generate-composite.sh <output-dir>

set -e

OUTPUT_DIR="${1:?Usage: $0 <output-dir>}"
mkdir -p "$OUTPUT_DIR"

# legacy/ contains all pre-Tycho versions
CHILDREN="    <child location=\"legacy/\"/>\n"
COUNT=1

# Add only version directories that actually exist (CI-deployed versions)
for dir in "$OUTPUT_DIR"/*/; do
  [ -d "$dir" ] || continue
  v=$(basename "$dir")
  [[ "$v" =~ ^[0-9]+\.[0-9]+\.[0-9]+ ]] || continue
  CHILDREN="${CHILDREN}    <child location=\"${v}/\"/>\n"
  COUNT=$((COUNT + 1))
done

TIMESTAMP=$(date +%s000)

cat > "$OUTPUT_DIR/compositeContent.xml" <<EOF
<?xml version='1.0' encoding='UTF-8'?>
<?compositeMetadataRepository version='1.0.0'?>
<repository name='ZipEditor Update Site' type='org.eclipse.equinox.internal.p2.metadata.repository.CompositeMetadataRepository' version='1.0.0'>
  <properties size='2'>
    <property name='p2.timestamp' value='${TIMESTAMP}'/>
    <property name='p2.compressed' value='true'/>
  </properties>
  <children size='${COUNT}'>
$(echo -e "$CHILDREN")  </children>
</repository>
EOF

cat > "$OUTPUT_DIR/compositeArtifacts.xml" <<EOF
<?xml version='1.0' encoding='UTF-8'?>
<?compositeArtifactRepository version='1.0.0'?>
<repository name='ZipEditor Update Site' type='org.eclipse.equinox.internal.p2.artifact.repository.CompositeArtifactRepository' version='1.0.0'>
  <properties size='2'>
    <property name='p2.timestamp' value='${TIMESTAMP}'/>
    <property name='p2.compressed' value='true'/>
  </properties>
  <children size='${COUNT}'>
$(echo -e "$CHILDREN")  </children>
</repository>
EOF

printf 'version=1\nmetadata.repository.factory.order=compositeContent.xml,\\!\nartifact.repository.factory.order=compositeArtifacts.xml,\\!\n' > "$OUTPUT_DIR/p2.index"

echo "Generated composite with ${COUNT} children:"
echo -e "$CHILDREN"
