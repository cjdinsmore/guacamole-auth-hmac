<?php
$username = 'calvinmclean';
$hostname = 'hostname';
$protocol = 'vnc';
$port = 5901;
if ( $protocol === 'ssh' )
  $port=22;

$id = rand( 1, 20000 );
$vncPass = 'password';
$secretKey = 'secret';

$timestamp = time() * 1000;

function signParams($timestamp, $protocol, $secretKey) {
  return base64_encode( hash_hmac( 'sha256', $timestamp . $protocol, $secretKey, 1 ) );
}
?>

<!DOCTYPE html>
<html>
  <head>
    <title>Guacamole Button</title>
  </head>
  <body>
    <form method="post" action="http://localhost:8888/guacamole/#/client" class="inline">

      <input type="hidden" name="id" value="<?php echo $id; ?>">
      <input type="hidden" name="timestamp" value="<?php echo $timestamp; ?>">
      <!-- <input type="hidden" name="guac.username" value="<?php echo $username; ?>"> -->
      <input type="hidden" name="guac.port" value="<?php echo $port; ?>">
      <input type="hidden" name="guac.hostname" value="<?php echo $hostname; ?>">
      <input type="hidden" name="guac.protocol" value="<?php echo $protocol; ?>">
      <input type="hidden" name="guac.password" value="<?php echo $vncPass; ?>">
      <input type="hidden" name="signature" value="<?php echo signParams($timestamp, $protocol, $secretKey); ?>">

      <button type="submit" name="submit_param" value="submit_value" class="link-button">
        CONNECT
      </button>

    </form>
  </body>
</html>
