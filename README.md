# AutoPix

 O AutoPix é um plugin Spigot que integra <b>código QR</b> PIX dentro do Minecraft e permite que os jogadores comprem itens e recebam automaticamente em questão de segundos.

## Como funciona?

O jogador digita `/comprarpix` e um menu será mostrado com os produtos disponíveis:

<img src="https://i.imgur.com/JEaNamt.png" align="middle" width="350px;">

e quando ele confirmar a compra receberá nas mãos um QR code para pagar por PIX:

<img src="https://i.imgur.com/UqkV1n4.png" align="middle" width="250px">

## Modos de validação
**Modo automático:** No modo automático o jogador só precisa aguardar a confirmação do pagamento. A cada `x` segundos 
o plugin faz uma verificação nos status dos pedidos pendentes e ativa ao ser aprovado. Quanto menor o tempo de
verificação mais instantânea será a confirmação, porém serão mais requisições para a API do MP, o que pode prejudicar o
desempenho do servidor se for muito baixo.

**Modo manual:** Após pagar pelo banco de preferência ele obtém o código do PIX e faz `/pix validar <Codigo>`. 
A única vantagem desse modo é que não cobra taxas, pois o PIX é realizado direto para a chave configurada. Ao validar é feito
a busca nas transações a partir do código E2E do PIX. Pode ter limitações se muitas transações forem realizadas, o mais
recomendado é o modo automático.

Você pode configurar comandos para dar VIP, dinheiro, itens, etc.
 

## Comandos
Além dos comandos já mencionados o plugin conta com o comando `/pix info` que abre um livro com instruções de como validar para os seus jogadores não terem dúvidas:

<img src="https://i.imgur.com/2IOTGDy.png" align="middle" width="250px">

Existe também o comando `/pix lista` que mostra a lista de ordens criadas pelo jogador e as informações. Para admins pode ser usado `/pix lista <Jogador>` para ver as ordens de outro jogador.

<img src="https://i.imgur.com/sEd6vXT.png" align="middle" width="400px">

Além desses, existem os comandos:
- `/pix reload` - Recarrega as configurações e mensagens do plugin
- `/comprarpix <Inventario>` - Abre um inventário específico. Na config é possível criar múltiplos inventários para vender
diferentes tipos de itens. O plugin não suporta paginação, é necessário colocar o comando em um NPC ou usar outro plugin de
inventários que execute o comando.

## Permissões
`autopix.use` - permite realizar compras por PIX

`autopix.admin` - permite ver a lista de pedidos de outros jogadores e dar reload no plugin

## Outras features
- Todas as mensagens editáveis
- Limite de tempo entre as ações para evitar sobrecarga no servidor
- Múltiplos menus

## Vídeos demonstrativos:
https://youtu.be/vVs14RqBq3Q

https://youtu.be/38rZIy0lXbM

## Download e instalação
1. Baixe a última versão em <a href="https://github.com/warleysr/autopix/releases">releases</a> e coloque na pasta `plugins` do seu servidor. 
2. Inicie o servidor para gerar os arquivos
3. Edite o arquivo `config.yml` colocando o Token do MercadoPago que pode ser obtido <a href="https://www.mercadopago.com.br/settings/account/credentials">aqui</a>
4. Configure a chave PIX que receberá os pagamentos (ela deve estar vinculada a sua conta do MP)
5. Configure também a conexão ao banco de dados 
6. Reinicie o servidor

---
Thanks to @rapust for creating <a href="https://github.com/rapust/QRCodeMap">QRCodeMap</a>.