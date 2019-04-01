# Cliente DNS
Proyecto final que hice para la asignatura Redes de Ordenadores de Teleco. Se trata de un cliente DNS que permite realizar consultas iterativas sobre la jerarquía de servidores DNS para poder resolver las consultas solicitadas por el usuario. 

# Utilización
`java Dnsclient {-t, -u} <IP_SERVIDOR_DNS>` donde `-u` significa realizar la consulta mediante UDP y `-t` mediante TCP, y `IP_SERVIDOR_DNS` la dirección IP del servidor por el que se comenzará la búsqueda iterativa. 

Las consultas se introducen por la entrada estándar, con el formato `RRType Nombre`, donde `RRType` es el tipo de recurso a consultar `(A, AAAA, NS, CNAME, MX, TXT)`. Las respuestas se mostrarán línea a línea, en formato pregunta-respuesta, con el siguiente formato:
<ul>
  <li> Q: Protocolo Servidor RRType Nombre
  <li> A: Servidor RRType TTL Valor
  </ul>

# Librerías externas
Este programa se basa en la librería <a href="https://github.com/RedesdeOrdenadores/LibDNSClient">LibDNSClient</a> desarrollada por <a href="https://github.com/migrax">Miguel Rodríguez</a> (DET de la Universidade de Vigo), ofrecida bajo licencia GNU GPL v3.
