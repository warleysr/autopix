# AutoPix

 O AutoPix é um plugin Spigot que integra <b>código QR</b> PIX dentro do Minecraft e permite que os jogadores comprem itens e recebam automaticamente em questão de segundos.

## Como funciona?

O jogador digita `/comprarpix` e um menu será mostrado com os produtos disponíveis:

<img src="https://i.imgur.com/JEaNamt.png" align="middle" width="350px;">

e quando ele confirmar a compra receberá nas mãos um QR code para pagar por PIX:

<img src="https://i.imgur.com/UqkV1n4.png" align="middle" width="250px">

## Modos de validação
**Modo manual:** Após pagar pelo banco de preferência ele obtém o código do PIX e faz `/pix validar <Codigo da Transacao>` e os comandos configurados serão executados. 

**Modo automático:** No modo automático o jogador só precisa aguardar a confirmação. Para isso será necessário configurar uma URL de notificação para o MercadoPago.

Você pode configurar comandos de dar VIP, dinheiro, itens etc.
 

## Comandos
Além dos comandos já mencionados o plugin conta com o comando `/pix info` que abre um livro com instruções de como validar para os seus jogadores não terem dúvidas:

<img src="https://i.imgur.com/2IOTGDy.png" align="middle" width="250px">

Existe também o comando `/pix lista` que mostra a lista de ordens criadas pelo jogador e as informações. Para admins pode ser usado `/pix lista <Jogador>` para ver as ordens de outro jogador.

<img src="https://i.imgur.com/sEd6vXT.png" align="middle" width="400px">

## Outras features
- Salvamento dos dados com MySQL
- Todas as mensagens editáveis
- Limite de tempo entre as ações para evitar sobrecarga no server

## Vídeos demonstrativos:
https://youtu.be/vVs14RqBq3Q

https://youtu.be/38rZIy0lXbM

## Download e instalação
1. Baixe a última versão em <a href="https://github.com/warleysr/autopix/releases">releases</a> e coloque na pasta `plugins` do seu servidor. 
2. Inicie o servidor para gerar os arquivos
3. Edite o arquivo `config.yml` colocando o Token do MercadoPago que pode ser obtido <a href="https://www.mercadopago.com.br/settings/account/credentials">aqui</a>
4. Configure a chave PIX que receberá os pagamentos (ela deve estar vinculada a sua conta do MP)
5. Configure também a conexão ao banco de dados MySQL 
6. Reinicie o servidor

## Configuração do modo automático
Se você deseja utilizar o modo 100% automático, siga estes passos:
1. Baixe o arquivo <a href="https://github.com/warleysr/autopix/blob/master/web/notification.php">web/notification.php</a> nesse repositório
2. Edite as primeiras linhas para configurar o banco de dados (o mesmo configurado no plugin) e o token de acesso do MP
3. Coloque este arquivo em sua hospedagem em algum diretório, por ex: `http://seusite.com/web/notification.php`
4. Configure essa URL na `config.yml` do plugin
5. Reinicie o servidor



Thanks to @rapust for creating <a href="https://github.com/rapust/QRCodeMap">QRCodeMap</a>.