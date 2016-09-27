<?php
$username = 'username';
$hostname = 'hostname';
$protocol = 'vnc';
$port = 5901;
if ( $protocol === 'ssh' )
  $port=22;

$id = rand( 1, 20000 );
$vncPass = 'password';
$secretKey = 'secret';

$timestamp = time() * 1000;
?>

<!DOCTYPE html>
<meta charset='UTF-8'>
<html>
  <head>
    <title>Guacamole Button</title>
  </head>
  <body>
    <form enctype='application/x-www-form-urlencoded' method='POST' action='http://localhost:8888/guacamole/#/client/<?= $id; ?>'>

      <input type='hidden' name='id' value='<?= urlencode('c/'.$id) ?>'>
      <input type='hidden' name='timestamp' value='<?= urlencode($timestamp) ?>'>
      <input type='hidden' name='guac.port' value='<?= urlencode($port) ?>'>
      <input type='hidden' name='guac.hostname' value='<?= urlencode($hostname) ?>'>
      <input type='hidden' name='guac.protocol' value='<?= urlencode($protocol) ?>'>
      <input type='hidden' name='guac.password' value='<?= urlencode($vncPass) ?>'>
      <input type='hidden' name='signature' value='<?php echo base64_encode( hash_hmac( 'sha256', $timestamp . $protocol, $secretKey, 1 ) ); ?>'>

      <input type='submit' value='click for Guacamole' >

    </form>
  </body>
</html>
