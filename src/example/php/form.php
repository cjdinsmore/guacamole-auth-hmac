<?php
  // Connection info
  $username  = 'username';
  $hostname  = 'localhost';
  $protocol  = 'ssh';
  $vncPass   = 'password';
  $secretKey = 'secret';
  $port      = 5901;

  $URL = "http://localhost:8888/guacamole";

  // SSH port is 22, not 5901
  if ($protocol === 'ssh') {
    $port = 22;
    $vncPass = '';
  }

  // ID is a random number
  $id = rand( 1, 20000 );

  // Create timestamp with current time
  $timestamp = round( time() * 1000 );

  // Concatenate timestamp and protocol for the signature
  $message = "$timestamp$protocol$hostname$port$username$vncPass";

  // Hash the message for the signature
  $signature = hash_hmac('sha256', $message, $secretKey, 1);

  // Make ID a string and cut off first 2 chars ( This is how connections always appear in Guacamole after connecting )
  $idX = strval($id);
  $idX = substr($idX, 2);

  $base64id = base64_encode( $idX . "\0" . 'c' . "\0" . 'hmac' );
?>

<!DOCTYPE html>
<meta charset='UTF-8'>
<html>
  <head>
    <title>Guacamole Button</title>
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.12.4/jquery.min.js"></script>
    <style>
      button {
        height: 50px;
        width: 100px;
        padding: 8px;
      }
    </style>
  </head>
  <body>
    <form enctype='application/x-www-form-urlencoded' method='POST' action='<?= $URL ?>/api/tokens' name='guacform'>
      <input type='hidden' name='timestamp'     value='<?= urlencode($timestamp) ?>'>
      <input type='hidden' name='guac.port'     value='<?= urlencode($port) ?>'>
      <input type='hidden' name='guac.username' value='<?= urlencode($username) ?>'>
      <input type='hidden' name='guac.password' value='<?= urlencode($vncPass) ?>'>
      <input type='hidden' name='guac.protocol' value='<?= urlencode($protocol) ?>'>
      <input type='hidden' name='signature'     value='<?= base64_encode($signature) ?>'>
      <input type='hidden' name='guac.hostname' value='<?= urlencode($hostname) ?>'>
      <input type='hidden' name='id'            value='<?= urlencode($id) ?>'>
    </form>

    <div id="guac">
      <button type="button" onclick="submitForm()" >Connect to Guacamole!</button>
    </div>

    <script>
    function submitForm() {
      var xhr = new XMLHttpRequest();

      // Turn form into a string
      var str = $("form").serialize();

      // Open the xhr
      xhr.open('POST','<?= $URL ?>/api/tokens', true);
      xhr.setRequestHeader("Content-type","application/x-www-form-urlencoded");

      // Send the form data
      xhr.send(str);

      // Redirect on successful response
      xhr.onreadystatechange=function() {
        if( xhr.readyState === 4 ) {
          if( xhr.status === 200 ) {
            // window.open( "<?= $URL ?>/#/client/c/<?php $id ?>?token=" + JSON.parse(xhr.responseText).authToken );
            window.open( "<?= $URL ?>/#/client/<?= $base64id ?>?token=" + JSON.parse(xhr.responseText).authToken );
          }
        }
      };
    }
    </script>
  </body>
</html>
