if exist unimportant_rsa_2048.keystore delete unimportant_rsa_2048.keystore
@rem -alias somealias 
keytool -genkey -v -keystore unimportant_rsa_2048.keystore -storepass somepassstore -keypass somepasskey -keyalg PKCS12 -keysize 2048 -validity 10000
@pause
