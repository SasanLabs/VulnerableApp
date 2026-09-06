---
layout: default
title: Portuguese
parent: Locale
---
# ![OWASP VulnerableApp](https://raw.githubusercontent.com/SasanLabs/VulnerableApp/master/docs/logos/Coloured/iconColoured.png) OWASP VulnerableApp

![OWASP Incubator](https://img.shields.io/badge/owasp-incubator-blue.svg) ![](https://img.shields.io/github/v/release/SasanLabs/VulnerableApp?style=flat) [![Licença](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0) ![Java CI with Gradle](https://github.com/SasanLabs/VulnerableApp/workflows/Java%20CI%20with%20Gradle/badge.svg) [![PRs bem-vindos](https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat-square)](http://makeapullrequest.com) [![Docker Pulls](https://badgen.net/docker/pulls/sasanlabs/owasp-vulnerableapp?icon=docker&label=pulls)](https://hub.docker.com/r/sasanlabs/owasp-vulnerableapp/) [![codecov](https://codecov.io/gh/SasanLabs/VulnerableApp/graph/badge.svg?token=DTS3PA8WXZ)](https://codecov.io/gh/SasanLabs/VulnerableApp)

## Quebre. Escaneie. Reproduza. Use como referência. Melhore.

O OWASP VulnerableApp é uma aplicação modular deliberadamente vulnerável, pensada principalmente para validar e comparar scanners de segurança com cenários de teste reproduzíveis, além de apoiar o aprendizado e a experimentação.

### 🔍 O que o diferencia
Ao contrário das aplicações vulneráveis tradicionais, o VulnerableApp foi desenhado como um ecossistema de segurança testável, e não como um app estático de treino.

### Ele permite:

- 🔬 Benchmark de scanners para ferramentas como Burp Suite, OWASP ZAP e motores DAST próprios
- 🧩 Design modular de vulnerabilidades, com novos cenários sem alterar os serviços centrais
- 📊 Testes de regressão de segurança entre versões e ambientes
- 🎯 Simulação realista de superfície de ataque para padrões modernos de aplicações web
- 🧪 Comportamento determinístico das vulnerabilidades, para resultados de scan repetíveis
- 🧠 Feito para engenheiros de segurança, pesquisadores e educadores

![Entire architecture stack](https://github.com/SasanLabs/VulnerableApp/blob/master/docs/logos/sasanlabs.png)


### O VulnerableApp ajuda você a:

- Validar como as ferramentas de segurança se comportam em padrões de vulnerabilidade conhecidos
- Montar ambientes controlados para experimentação em segurança
- Ampliar a cobertura de vulnerabilidades à medida que novas técnicas de ataque aparecem
- Executar pipelines de teste de segurança consistentes e repetíveis

### ⚙️ Por que isso importa

A maioria das aplicações vulneráveis é:
- Estática
- Difícil de estender
- Feita só para aprendizado manual

### O VulnerableApp foi feito para:
automação, reprodutibilidade e evolução

### Interface do usuário ###
![VulnerableApp-facade UI](https://raw.githubusercontent.com/SasanLabs/VulnerableApp-facade/main/docs/images/gif/VulnerableApp-Facade.gif)

## Executando o projeto
Há 2 formas de executar o projeto:
1. A forma mais simples é usar containers Docker, que sobem a VulnerableApplication completa com todos os componentes. Para rodar como aplicação Docker, siga os passos:
    1. Baixe e instale o [Docker Compose](https://docs.docker.com/compose/install/)
    2. Clone este repositório no GitHub
    3. Abra o terminal e vá para o diretório raiz do projeto
    4. Execute o comando ```docker-compose pull && docker-compose up```
    5. Abra o navegador em `http://localhost` para ver a interface do VulnerableApp.
    6. O Mailpit também fica em `http://localhost/mailpit/` para ver os e-mails capturados pelo SMTP local.

    **Nota**: Os passos acima executam a versão mais recente ainda não lançada do VulnerableApp. Se quiser a última versão lançada, use a tag Docker **latest**.
2. Outra forma de executar o VulnerableApp é como aplicação vulnerável standalone:
    1. Vá até a [seção de Releases](https://github.com/SasanLabs/VulnerableApp/releases) no GitHub e baixe o Jar da última versão lançada
    2. Abra o terminal e vá para o diretório raiz do projeto
    3. Execute o comando ```java -jar VulnerableApp-*```
    4. Abra o navegador em `http://localhost:9090/VulnerableApp`. Essa é a interface legado do VulnerableApp.

## Compilando o projeto
Há 2 formas de compilar e usar este projeto:
1. Como aplicação Docker, para rodar a VulnerableApplication completa. Siga os passos:
    1. Gere a imagem Docker com `./gradlew jibDockerBuild`
    2. Baixe o [Docker-Compose](https://github.com/SasanLabs/VulnerableApp-facade/blob/main/docker-compose.yml) e, no mesmo diretório, execute `docker-compose up`
    3. Abra o navegador em `http://localhost` para ver a interface do VulnerableApp.
2. Como aplicação Spring Boot, que sobe com a UI legado ou a API REST e facilita o debug. O caminho simples:
    1. Importe o projeto na sua IDE e execute
    2. Abra o navegador em: `http://localhost:9090/VulnerableApp` para usar a interface legado no debug e nos testes.

## Contribuindo com o projeto

Há várias formas de contribuir:
1. Se você é desenvolvedor e está começando, o melhor é olhar a lista de [issues](https://github.com/SasanLabs/VulnerableApp/issues) com a label `good first issue`.
2. Se você é desenvolvedor ou profissional de segurança e quer adicionar um novo tipo de vulnerabilidade, gere o modelo com `./gradlew GenerateSampleVulnerability`. Isso cria um template com placeholders e comentários. Os arquivos alterados aparecem no log do comando ou no histórico do Git. Preencha os placeholders e compile o projeto para ver o efeito.
3. Se quiser contribuir divulgando o projeto ou ajudando no crescimento, deixe suas ideias na seção de discussions ou nas issues para conversarmos.

## Testando com a UI moderna
O VulnerableApp-facade oferece uma UI moderna para o VulnerableApp. Para testar suas alterações locais com essa UI:

1. **Pré-requisito**: Docker e Docker-Compose instalados.
2. **Execute o script de teste**:
   - No Windows: `.\\scripts\\testWithModernUI.bat`
   - No Linux/Mac: `./scripts/testWithModernUI.sh`

Esse script empacota suas alterações locais numa imagem Docker (`sasanlabs/owasp-vulnerableapp:unreleased`) e sobe o stack completo (incluindo facade, jsp e php) com `docker-compose.local.yml`.

3. **Acesse a UI**: abra `http://localhost` para ver a UI moderna com as suas mudanças.

4. **Acesse o Mailpit**: abra `http://localhost/mailpit/` para ver os e-mails capturados pelo SMTP local.

## Tecnologias usadas
- Java17
- Spring Boot
- ReactJS
- Javascript/TypeScript

### Conectando ao banco H2 embutido
Para acessar o banco pelo navegador: `http://localhost:9090/VulnerableApp/h2`

Propriedades de conexão:
```properties
JDBC Url: jdbc:h2:mem:testdb
User Name: admin
Password: hacker
```

## Tipos de vulnerabilidade já cobertos

1. [JWT Vulnerability](https://github.com/SasanLabs/VulnerableApp/blob/master/src/main/java/org/sasanlabs/service/vulnerability/jwt/)
2. [Command Injection](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/commandInjection)
3. [Cryptography Failures](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/cryptographicFailures)
4. [File Upload Vulnerability](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/fileupload)
5. [Path Traversal Vulnerability](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/pathTraversal)
6. [SQL Injection](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/sqlInjection)
    1. [Error Based SQLi](https://github.com/SasanLabs/VulnerableApp/blob/master/src/main/java/org/sasanlabs/service/vulnerability/sqlInjection/ErrorBasedSQLInjectionVulnerability.java)
    2. [Union Based SQLi](https://github.com/SasanLabs/VulnerableApp/blob/master/src/main/java/org/sasanlabs/service/vulnerability/sqlInjection/UnionBasedSQLInjectionVulnerability.java)
    3. [Blind SQLi](https://github.com/SasanLabs/VulnerableApp/blob/master/src/main/java/org/sasanlabs/service/vulnerability/sqlInjection/BlindSQLInjectionVulnerability.java)
7. [XSS](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/xss)
    1. [Persistent XSS](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/xss/persistent)
    2. [Reflected XSS](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/xss/reflected)
8. [XXE](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/xxe)
9. [Open Redirect](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/openRedirect)
    1. [Http 3xx Status code based](https://github.com/SasanLabs/VulnerableApp/blob/master/src/main/java/org/sasanlabs/service/vulnerability/urlRedirection/Http3xxStatusCodeBasedInjection.java)
10. [SSRF](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/ssrf)
11. [IDOR](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/idor)
12. [Clickjacking](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/clickjacking)
13. [LDAP Injection](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/ldapInjection)
14. [Authentication Vulnerability](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/authentication)
15. [Password Reset Vulnerability](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/passwordReset)

## Avaliando o seu scanner

O VulnerableApp inclui um comparador que nota os achados de um scanner contra a
verdade de referência do projeto e grava um relatório de cobertura / missed / unmatched.
Scanners DAST e SAST usam o mesmo endpoint:

- Endpoint: `POST http://<baseurl>/VulnerableApp/scanner/benchmark`
- Corpo da requisição — use o formato do seu scanner:
  - DAST: `{ tool, scanType: "DAST", findings: [ { url, type, cwe, wascId } ] }` (`scanType` é opcional e o padrão é `DAST`; `type`/`cwe`/`wascId` são opcionais individualmente — basta um eixo bater)
  - SAST: `{ tool, scanType: "SAST", findings: [ { filePath, line, cwe, type } ] }`
- Corpo da resposta e `benchmarks/<tool>-results.json` em disco: relatório de cobertura

Rodar o scanner em si fica fora do escopo — você envia o JSON. Veja
[`benchmarks/README.md`](../../../benchmarks/README.md) para os schemas de
entrada/saída, regras de matching, vocabulário canônico de tipos de
vulnerabilidade e exemplos de `curl`.

## Contato
Se travar em algum passo ou na compreensão do projeto e dos objetivos, envie um e-mail para karan.sasan@owasp.org ou abra uma [issue](https://github.com/SasanLabs/VulnerableApp/issues) que tentaremos ajudar.

## Documentação e referências

1. [OWASP Spotlight series overview of project](https://m.youtube.com/watch?v=hoCxzQQugZc&list=PLUKo5k_oSrfOTl27gUmk2o-NBKvkTGw0T&pp=iAQB)
2. [Overview video for OWASP Spotlight series](https://www.youtube.com/watch?v=HRRTrnRgMjs)
3. [Documentation](https://sasanlabs.github.io/VulnerableApp)
4. [Design Documentation](https://sasanlabs.github.io/VulnerableApp/DesignDocumentation.html)
5. [Owasp VulnerableApp](https://owasp.org/www-project-vulnerableapp/)
6. [Overview Video](https://www.youtube.com/watch?v=AjL4B-WwrrA&ab_channel=OwaspVulnerableApp)

### Blogs
1. [Overview of Owasp-VulnerableApp - Medium article](https://hussaina-begum.medium.com/an-extensible-vulnerable-application-for-testing-the-vulnerability-scanning-tools-cc98f0d94dbc)
2. [Overview of Owasp-VulnerableApp - Blogspot post](https://hussaina-begum.blogspot.com/2020/10/an-extensible-vulnerable-application.html)
3. [Introduction to Owasp VulnerableApp by Kenji Nakajima](https://jpn.nec.com/cybersecurity/blog/220520/index.html)
4. [Gen AI based platform Shannon exploiting VulnerableApp](https://qiita.com/fiord/items/9351bcff6d646862f181)
5. [I Built the OWASP ZAP File Upload Addon. Here’s Why VulnerableApp-Facade Had to Exist First](https://medium.com/p/52c4f2226ed3)

### Uso do OWASP VulnerableApp
1. [View Global Academic Interest](../../Usage.md)

### Referências de troubleshooting
1. [Reddit exploiting SQL Injection Vulnerability](https://www.reddit.com/r/hacking/comments/11wtf17/owasp_vulnerableappfacade_sql_injection/)

### README em outros idiomas

1. [Russian](https://github.com/SasanLabs/VulnerableApp/tree/master/docs/i18n/ru/README.md)
2. [Chinese](https://github.com/SasanLabs/VulnerableApp/tree/master/docs/i18n/zh-CN/README.md)
3. [Hindi](https://github.com/SasanLabs/VulnerableApp/tree/master/docs/i18n/hi/README.md)
4. [Punjabi](https://github.com/SasanLabs/VulnerableApp/tree/master/docs/i18n/pa/README.md)
5. [Korean](https://github.com/SasanLabs/VulnerableApp/tree/master/docs/i18n/ko/README.md)
6. [Portuguese (Brasil)](https://github.com/SasanLabs/VulnerableApp/tree/master/docs/i18n/pt-BR/README.md)
