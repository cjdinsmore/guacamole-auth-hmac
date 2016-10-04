<?php
$username = 'username';
$hostname = 'hostname';
$protocol = 'vnc';
$port = 5901;
if ($protocol === 'ssh')
  $port = 22;
$id = rand( 1,20000);
$vncPass = 'password';
$secretKey = 'secret';
$timestamp = round(time() * 1000);
$message = "$timestamp$protocol";

$signature = hash_hmac('sha256', $message, $secretKey, 1);
?>

<!DOCTYPE html>
<meta charset='UTF-8'>
<html>
  <head>
    <title>Guacamole Button</title>
  </head>
  <body>
    <form enctype='application/x-www-form-urlencoded' method='POST' action='http://localhost:8888/guacamole/#/c/client/<?= $id; ?>'>

      <input type='hidden' name='timestamp'     value='<?= urlencode($timestamp) ?>'>
      <input type='hidden' name='guac.port'     value='<?= urlencode($port) ?>'>
      <input type='hidden' name='guac.password' value='<?= urlencode($vncPass) ?>'>
      <input type='hidden' name='guac.protocol' value='<?= urlencode($protocol) ?>'>
      <input type='hidden' name='signature'     value='<?= base64_encode($signature) ?>'>
      <input type='hidden' name='guac.hostname' value='<?= urlencode($hostname) ?>'>
      <input type='hidden' name='id'            value='<?= urlencode('c/'.$id) ?>'>

      <input type='submit' value='click for Guacamole' >
    </form>
  </body>
</html>
