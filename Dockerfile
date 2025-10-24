# -----------------------------------------------------------------
# STAGE 1: BUILD (Compila a aplicação usando Maven)
# -----------------------------------------------------------------
# Usa uma imagem base com o Maven e a JDK para compilação
FROM maven:3.9.6-eclipse-temurin-17 AS builder

# Define o diretório de trabalho no container
WORKDIR /app

# Copia os arquivos de configuração do Maven e as dependências
COPY pom.xml .
COPY .mvn .mvn
RUN mvn dependency:resolve

# Copia o código fonte e faz o build (package)
COPY src src
RUN mvn clean package -DskipTests

# -----------------------------------------------------------------
# STAGE 2: RUNTIME (Roda a aplicação em uma imagem mais leve)
# -----------------------------------------------------------------
# Usa uma imagem base mais leve, contendo apenas a JRE (Java Runtime Environment)
FROM eclipse-temurin:17-jre-alpine

# Define o argumento para o nome do arquivo JAR (o Maven o nomeia com 'version-SNAPSHOT')
# Você deve ajustar o nome do seu JAR aqui ou usar curinga se for um Fat JAR do Spring Boot
ARG JAR_FILE=/app/target/*.jar

# Copia o JAR do estágio 'builder' para o estágio 'runtime'
COPY --from=builder ${JAR_FILE} app.jar

# Expõe a porta que a aplicação usa (8080)
EXPOSE 8080

# Comando para executar a aplicação
ENTRYPOINT ["java", "-jar", "app.jar"]