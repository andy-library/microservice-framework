#!/usr/bin/env bash

set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

expected_starters=(
  common json logging nacos apollo observability database redis kafka elasticsearch
  async xxl-job web feign security drools audit field-encryption object-storage
)

# ────────────────────────────────────────────────────────────────────
# 1. 安装 Parent 制品
# ────────────────────────────────────────────────────────────────────
mvn -q -f "${project_dir}/pom.xml" install -DskipTests

# ────────────────────────────────────────────────────────────────────
# 2. 正面契约：消费方项目可以解析依赖
# ────────────────────────────────────────────────────────────────────
echo "Validating positive contract tests..."
mvn -q -f "${project_dir}/src/it/business-parent-resolution/pom.xml" validate
mvn -q -f "${project_dir}/src/it/framework-bom-resolution/pom.xml" validate
mvn -q -f "${project_dir}/src/it/no-runtime-starter-inheritance/pom.xml" validate
mvn -q -f "${project_dir}/src/it/framework-starter-inherits-parent/pom.xml" validate
mvn -q -f "${project_dir}/src/it/business-app-resolves-starters/pom.xml" validate
mvn -q -f "${project_dir}/src/it/app-fat-jar-packaging/pom.xml" package

# ────────────────────────────────────────────────────────────────────
# 3. 负面契约：banned dependency 应导致构建失败
# ────────────────────────────────────────────────────────────────────
echo "Validating negative contract: banned dependency..."
if mvn -q -f "${project_dir}/src/it/banned-dependency-fails/pom.xml" validate 2>/dev/null; then
  echo "Banned dependency contract failed: log4j:log4j should be rejected by enforcer rules." >&2
  exit 1
fi
echo "✓ Banned dependency (log4j:log4j) correctly rejected."

# ────────────────────────────────────────────────────────────────────
# 4. Starter Parent 不引入运行时 Starter 依赖
# ────────────────────────────────────────────────────────────────────
runtime_tree="$(
  mvn -q -f "${project_dir}/src/it/no-runtime-starter-inheritance/pom.xml" \
    dependency:tree -Dincludes=com.microservice.framework -DoutputType=text
)"

if [[ "${runtime_tree}" == *"-starter:"* ]]; then
  echo "Starter Parent must not introduce runtime Starter dependencies." >&2
  echo "${runtime_tree}" >&2
  exit 1
fi
echo "✓ Starter Parent does not introduce runtime Starter dependencies."

# ────────────────────────────────────────────────────────────────────
# 5. Root Parent 不得管理运行时依赖
# ────────────────────────────────────────────────────────────────────
root_model_before_build="$(
  sed -n '1,/<build>/p' "${project_dir}/pom.xml"
)"

if echo "${root_model_before_build}" | grep -q '<dependencyManagement>'; then
  echo "Root parent must not declare dependencyManagement. Use microservice-framework-dependencies and microservice-framework-bom instead." >&2
  exit 1
fi

if echo "${root_model_before_build}" | grep -q '<dependencies>'; then
  echo "Root parent must not declare runtime dependencies. It is a build/governance parent only." >&2
  exit 1
fi
echo "✓ Root Parent does not declare runtime dependencies or dependencyManagement."

# ────────────────────────────────────────────────────────────────────
# 6. BOM 管理且仅管理 19 个批准的正式 Starter 坐标
# ────────────────────────────────────────────────────────────────────
for starter in "${expected_starters[@]}"; do
  artifact="microservice-framework-${starter}-starter"
  if ! grep -q "<artifactId>${artifact}</artifactId>" "${project_dir}/microservice-framework-bom/pom.xml"; then
    echo "Framework BOM is missing official Starter coordinate: ${artifact}" >&2
    exit 1
  fi
done

starter_count="$(
  grep -E -c '<artifactId>microservice-framework-[a-z0-9-]+-starter</artifactId>' \
    "${project_dir}/microservice-framework-bom/pom.xml"
)"

if [[ "${starter_count}" -ne 19 ]]; then
  echo "Framework BOM must manage exactly 19 official Starter coordinates; found ${starter_count}." >&2
  exit 1
fi
echo "✓ BOM manages exactly 19 official Starter coordinates."

# ────────────────────────────────────────────────────────────────────
# 7. BOM 不含第三方依赖坐标（职责边界）
# ────────────────────────────────────────────────────────────────────
bom_non_framework="$(
  sed -n '/<dependencyManagement>/,/<\/dependencyManagement>/p' "${project_dir}/microservice-framework-bom/pom.xml" \
    | grep '<groupId>' \
    | grep -v 'com.microservice.framework' \
    | grep -v '<!--' \
    || true
)"
if [[ -n "${bom_non_framework}" ]]; then
  echo "Framework BOM must not contain third-party dependency coordinates. Found:" >&2
  echo "${bom_non_framework}" >&2
  exit 1
fi
echo "✓ BOM contains only Framework internal coordinates."

# ────────────────────────────────────────────────────────────────────
# 8. Dependencies 不含内部 Starter 坐标（职责边界）
# ────────────────────────────────────────────────────────────────────
deps_internal="$(
  sed -n '/<dependencyManagement>/,/<\/dependencyManagement>/p' "${project_dir}/microservice-framework-dependencies/pom.xml" \
    | grep '<artifactId>' \
    | grep 'microservice-framework-' \
    || true
)"
if [[ -n "${deps_internal}" ]]; then
  echo "Dependencies BOM must not contain Framework internal Starter coordinates. Found:" >&2
  echo "${deps_internal}" >&2
  exit 1
fi
echo "✓ Dependencies contains only third-party coordinates."

# ────────────────────────────────────────────────────────────────────
# 9. 业务入口必须导入外部依赖 BOM 和内部组件 BOM
# ────────────────────────────────────────────────────────────────────
for required_bom in microservice-framework-dependencies microservice-framework-bom; do
  if ! grep -q "<artifactId>${required_bom}</artifactId>" "${project_dir}/microservice-framework-starter-parent/pom.xml"; then
    echo "Starter Parent must import ${required_bom}." >&2
    exit 1
  fi
done
echo "✓ Starter Parent imports both external dependency BOM and Framework BOM."

# ────────────────────────────────────────────────────────────────────
# 10. 现有 Starter 不生成 Fat JAR（不含 repackage）
# ────────────────────────────────────────────────────────────────────
for starter_project in "${project_dir}/../microservice-framework-observability-starter" "${project_dir}/../microservice-framework-logging-starter"; do
  if [[ -d "${starter_project}" ]]; then
    effective_pom="$(mvn -q -f "${starter_project}/pom.xml" help:effective-pom 2>/dev/null || true)"
    if echo "${effective_pom}" | grep -q 'spring-boot-maven-plugin' && echo "${effective_pom}" | grep -q 'repackage'; then
      echo "Starter project must not use spring-boot-maven-plugin:repackage. Found in: ${starter_project}" >&2
      exit 1
    fi
  fi
done
echo "✓ Starter projects do not use spring-boot-maven-plugin:repackage."

# ────────────────────────────────────────────────────────────────────
# 11. Enforcer 规则生效
# ────────────────────────────────────────────────────────────────────
echo "Validating enforcer rules..."
mvn -q -f "${project_dir}/pom.xml" validate
echo "✓ All enforcer rules pass."

# ────────────────────────────────────────────────────────────────────
echo "Parent consumer contracts verified."
