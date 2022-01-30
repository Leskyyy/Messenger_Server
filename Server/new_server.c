#include <sys/socket.h>
#include <stdio.h>
#include <string.h>
#include <arpa/inet.h>
#include <pthread.h>
#include <stdbool.h>
#include <stdlib.h>
#include <sqlite3.h>
#include <stdint.h>
#define MAX_NUMBER_OF_CLIENTS 20

const int INT_SIZE = 4;

typedef struct Message {
    int totalLen;
    char* sender;
    char* receiver;
    char* content;
}Message;

typedef struct Credentials {
    char* login;
    char* password;
}Credentials;

typedef struct Request {
    char* name;
}Request;

pthread_mutex_t mutex;
int clients[20];
int n = 0;
int number_of_clients = 0;

void recvmg(void* socket);

static int callback(void *NotUsed, int argc, char **argv, char **azColName) {
   int i;
   for(i = 0; i<argc; i++) {
      printf("%s = %s\n", azColName[i], argv[i] ? argv[i] : "NULL");
   }
   printf("\n");
   return 0;
}

int get_receiver_socket(char* receiver){

    sqlite3 *db;
    int socket = -1;

    int rc = sqlite3_open("database.db", &db);
   
    if( rc ) {
        fprintf(stderr, "Can't open database: %s\n", sqlite3_errmsg(db));
        return(0);
    } else {
        fprintf(stdout, "Opened database successfully\n");
    }

    sqlite3_stmt *stmt;
    char sql[100];
    strcpy(sql,"SELECT SOCKET FROM USERS WHERE LOGIN = '");
    strcat(sql, receiver);
    strcat(sql, "'");

    rc = sqlite3_prepare_v2(db, sql, -1, &stmt, 0);

    if (rc != SQLITE_OK) {
        printf("Error, sql!\n");
    }
    while ((rc = sqlite3_step(stmt)) == SQLITE_ROW) {
        socket = sqlite3_column_int(stmt, 0);
        sqlite3_finalize(stmt);
        sqlite3_close(db);
        return socket;
    }
    return socket;
}

int getLengthFromBytes(char* lenBytes) {
    return (lenBytes[0] << 24) | (lenBytes[1] << 16) | (lenBytes[2] << 8) | lenBytes[3];
}

void setBytesFromLength(char* buffer, int length) {
    buffer[0] = (length >> 24) & 0xFF;
    buffer[1] = (length >> 16) & 0xFF;
    buffer[2] = (length >> 8) & 0xFF;
    buffer[3] = length & 0xFF;
}

char* allocMemForString(int len) {
    char* arr = (char*)malloc((size_t)(len * sizeof(char)));
    memset(arr, '\0', len);

    return arr;
}

int getMessageLength(Message message) {
    return 4 + 4 + strlen(message.sender) + 4 + strlen(message.receiver) + 4 + strlen(message.content);
}

void encode(char* output, Message message) {

    int totalLen = message.totalLen;

    int cursor = 0;
    setBytesFromLength(output + cursor, totalLen); cursor += INT_SIZE;

    const int senderLen = strlen(message.sender);
    setBytesFromLength(output + cursor, senderLen); cursor += INT_SIZE;
    strncpy(output + cursor, message.sender, senderLen); cursor += senderLen;

    const int receiverLen = strlen(message.receiver);
    setBytesFromLength(output + cursor, receiverLen); cursor += INT_SIZE;
    strncpy(output + cursor, message.receiver, receiverLen); cursor += receiverLen;

    const int contentLen = strlen(message.content);
    setBytesFromLength(output + cursor, contentLen); cursor += INT_SIZE;
    strncpy(output + cursor, message.content, contentLen); cursor += contentLen;

    printf("Total: %d, sender: %d, receiver: %d, content: %d\n", totalLen, senderLen, receiverLen, contentLen);

}

Message decode(char* encodedMessage) {
    int cursor = 0;

    int senderLen = getLengthFromBytes(encodedMessage + cursor); cursor += INT_SIZE;
    char* sender = allocMemForString(senderLen + 1);
    strncpy(sender, encodedMessage + cursor, senderLen+1); cursor += senderLen;
    sender[senderLen] = '\0';
    

    int receiverLen = getLengthFromBytes(encodedMessage + cursor); cursor += INT_SIZE;
    char* receiver = allocMemForString(receiverLen + 1);
    strncpy(receiver, encodedMessage + cursor, receiverLen); cursor += receiverLen;
    receiver[receiverLen] = '\0';

    int contentLen = getLengthFromBytes(encodedMessage + cursor); cursor += INT_SIZE;
    char* content = allocMemForString(contentLen + 1);
    strncpy(content, encodedMessage + cursor, contentLen); cursor += contentLen;
    content[contentLen] = '\0';

    Message message;

    message.sender = sender;
    message.receiver = receiver;
    message.content = content;
    message.totalLen = getMessageLength(message);
    
    return message;
    free(sender);
    free(receiver);
    free(content);
}

void encodeCredentials(char* output, Credentials credentials) {

    int cursor = 0;

    const int loginLen = strlen(credentials.login);
    setBytesFromLength(output + cursor, loginLen); cursor += INT_SIZE;
    strncpy(output + cursor, credentials.login, loginLen); cursor += loginLen;

    const int passwordLen = strlen(credentials.password);
    setBytesFromLength(output + cursor, passwordLen); cursor += INT_SIZE;
    strncpy(output + cursor, credentials.password, passwordLen); cursor += passwordLen;

}

void encodeRequest(char *output, Request request, int decision){
    output[0] = 'r';
    if (decision == 1){
        output[1] = '1';
    }
    else if(decision == 0){
        output[1] = '0';
    }
    int cursor = 2;
    const int loginLen = strlen(request.name);
    setBytesFromLength(output + cursor, loginLen); cursor += INT_SIZE;
    strncpy(output + cursor, request.name, loginLen); cursor += loginLen;
}

Credentials decodeCredentials(char* encodedCredentials, bool to_register) {
 
    int cursor = 1;

    int loginLen = getLengthFromBytes(encodedCredentials + cursor); cursor += INT_SIZE;
    char* login = allocMemForString(loginLen + 1);
    strncpy(login, encodedCredentials + cursor, loginLen+1); cursor += loginLen;
    login[loginLen] = '\0';
    

    int passwordLen = getLengthFromBytes(encodedCredentials + cursor); cursor += INT_SIZE;
    char* password = allocMemForString(passwordLen + 1);
    strncpy(password, encodedCredentials + cursor, passwordLen+1); cursor += passwordLen;
    password[passwordLen] = '\0';

    Credentials credentials;

    credentials.login = login;
    credentials.password = password;

    return credentials;

    free(login);
    free(password);
}

Request decodeRequest(char* encodedMessage) {
    int cursor = 0;

    cursor += 1;
    int nameLen = getLengthFromBytes(encodedMessage + cursor); cursor += INT_SIZE;
    char *name = allocMemForString(nameLen + 1);
    strncpy(name, encodedMessage + cursor, nameLen+1); cursor += nameLen;
    name[nameLen] = '\0';

    Request request;

    request.name = name;

    return request;
    
    free(name);
}

int check_user_in_database(char* login_to_check){

    sqlite3 *db;
    int count = -1;

    int rc = sqlite3_open("database.db", &db);
   
    if( rc ) {
        fprintf(stderr, "Can't open database: %s\n", sqlite3_errmsg(db));
        return(0);
    } else {
        fprintf(stdout, "Opened check user in database successfully\n");
    }

    sqlite3_stmt *stmt;
    char sql[100];
    strcpy(sql,"SELECT COUNT(*) FROM USERS WHERE LOGIN = '");
    strcat(sql, login_to_check);
    strcat(sql, "'");

    printf("SQL query: %s\n", sql);

    rc = sqlite3_prepare_v2(db, sql, -1, &stmt, 0);

    if (rc != SQLITE_OK) {
        printf("Error, sql!\n");
    }

    while ((rc = sqlite3_step(stmt)) == SQLITE_ROW) {
        count = sqlite3_column_int(stmt, 0);
        sqlite3_finalize(stmt);
        if(count == 1){
            printf("Znaleziono takiego uzytkownika!\n");
            return 1;
        }
        else{
            printf("Nie znaleziono takiego uzytkownika!\n");
            return 0;
        }
    }

    return 0;
}


int check_login_details(Credentials credentials){

    sqlite3 *db;
    int count = -1;

    int rc = sqlite3_open("database.db", &db);
   
    if( rc ) {
        fprintf(stderr, "Can't open database: %s\n", sqlite3_errmsg(db));
        return(0);
    } else {
        fprintf(stdout, "Opened database successfully\n");
    }

    sqlite3_stmt *stmt;
    char sql[100];
    char sql2[100];
    strcpy(sql,"SELECT COUNT(*) FROM USERS WHERE LOGIN = '");
    strcat(sql, credentials.login);
    strcat(sql, "'");

    rc = sqlite3_prepare_v2(db, sql, -1, &stmt, 0);

    if (rc != SQLITE_OK) {
        printf("Error, sql!\n");
    }

    while ((rc = sqlite3_step(stmt)) == SQLITE_ROW) {
        count = sqlite3_column_int(stmt, 0);
        sqlite3_finalize(stmt);
        if(count == 1){
            printf("Znaleziono login!\n");
            break;
        }
        else{
            printf("Nie znaleziono loginu!\n");
            return 0;
        }
    }

    strcpy(sql2,"SELECT COUNT(*) FROM USERS WHERE LOGIN = '");
    strcat(sql2, credentials.login);
    strcat(sql2, "' AND PASSWORD = '");
    strcat(sql2, credentials.password);
    strcat(sql2, "'");

    printf("SQl query: %s\n", sql2);

    rc = sqlite3_prepare_v2(db, sql2, -1, &stmt, 0);

    if (rc != SQLITE_OK) {
        printf("Error, sql!\n");
    }

    while ((rc = sqlite3_step(stmt)) == SQLITE_ROW) {
        count = sqlite3_column_int(stmt, 0);
        sqlite3_finalize(stmt);
        if(count == 1){
            printf("Login i haslo pasuja!\n");
            return 2;
        }
        else{
            printf("Login i haslo nie pasuja!\n");
            return 1;
        }
    }

    return 0;

}

int register_user(Credentials credentials, int sock){
    sqlite3 *db;
    int count = -1;
    char *zErrMsg = 0;
    char socket_char[20];
    sprintf(socket_char,"%d", sock);

    int rc = sqlite3_open("database.db", &db);
   
    if( rc ) {
        fprintf(stderr, "Can't open database: %s\n", sqlite3_errmsg(db));
        return(0);
    } else {
        fprintf(stdout, "Opened database successfully\n");
    }

    sqlite3_stmt *stmt;
    char sql[100];
    char sql2[100];
    strcpy(sql,"SELECT COUNT(*) FROM USERS WHERE LOGIN = '");
    strcat(sql, credentials.login);
    strcat(sql, "'");

    rc = sqlite3_prepare_v2(db, sql, -1, &stmt, 0);

    if (rc != SQLITE_OK) {
        printf("Error, sql!\n");
    }

    while ((rc = sqlite3_step(stmt)) == SQLITE_ROW) {
        count = sqlite3_column_int(stmt, 0);
        sqlite3_finalize(stmt);
        if(count == 1){
            printf("Uzytkownik o takim loginie juz istnieje!\n");
            return 0;
        }
        else{
            printf("Nazwa uzytkownika wolna!\n");
            break;
        }
    }

    strcpy(sql2, "INSERT INTO USERS (LOGIN, PASSWORD, SOCKET) VALUES ('");
    strcat(sql2, credentials.login);
    strcat(sql2, "','");
    strcat(sql2, credentials.password);
    strcat(sql2, "',");
    strcat(sql2, socket_char);
    strcat(sql2, ")");

    printf("SQL QRY: %s\n", sql2);

    rc = sqlite3_exec(db, sql2, callback, 0, &zErrMsg);

    if( rc != SQLITE_OK ){
        fprintf(stderr, "SQL error: %s\n", zErrMsg);
        sqlite3_free(zErrMsg);
    } else {
        fprintf(stdout, "Register procedure completed!\n");
    }
    sqlite3_close(db);
    return 1;
}

void sendtodatabase(char *sender, char *receiver, char *content){

    sqlite3 *db;
    char *zErrMsg = 0;
    int rc;
    char *sql;

    rc = sqlite3_open("database.db", &db);

    if( rc ) {
        fprintf(stderr, "Can't open database: %s\n", sqlite3_errmsg(db));
    } else {
        fprintf(stderr, "Opened database successfully\n");
    }

    sql = "INSERT INTO MESSAGES (FROM_USER,TO_USER,CONTENT) VALUES ('";
    strcat(sql, sender);
    strcat(sql, "','");
    strcat(sql, receiver);
    strcat(sql, "','");
    strcat(sql, content);
    strcat(sql, "')");

    rc = sqlite3_exec(db, sql, callback, 0, &zErrMsg);

    if( rc != SQLITE_OK ){
        fprintf(stderr, "SQL error: %s\n", zErrMsg);
        sqlite3_free(zErrMsg);
    } else {
        fprintf(stdout, "Records created successfully\n");
    }
    sqlite3_close(db);
}

void sendtoreceiver(char *sender, char *receiver, char *msg){

    int receiver_socket = get_receiver_socket(receiver);
    printf("Receiver socket: %d\n", receiver_socket);

    Message new_message;
    new_message.sender = sender;
    new_message.receiver = receiver;
    new_message.content = msg;
    new_message.totalLen = getMessageLength(new_message);
    char *output = allocMemForString(new_message.totalLen);
    printf("Message len: %d\n", new_message.totalLen);

    encode(output, new_message);

    

    if(send(receiver_socket,output,new_message.totalLen,0) < 0) {
        printf("Sending failure\n");
    }else{
        printf("I have forwarded the message!\n\n");
    }

}


void update_user_socket(char *login, int socket){

    sqlite3 *db;
    char socket_char[20];
    char *zErrMsg = 0;

    sprintf(socket_char,"%d", socket);

    int rc = sqlite3_open("database.db", &db);
   
    if( rc ) {
        fprintf(stderr, "Can't open database: %s\n", sqlite3_errmsg(db));
    } else {
        fprintf(stdout, "Opened database successfully\n");
    }

    char sql[100];
    strcpy(sql, "UPDATE USERS SET SOCKET = ");
    strcat(sql, socket_char);
    strcat(sql, " WHERE LOGIN = '");
    strcat(sql, login);
    strcat(sql, "'");

    rc = sqlite3_exec(db, sql, callback, 0, &zErrMsg);
   
    if( rc != SQLITE_OK ){
        fprintf(stderr, "SQL error: %s\n", zErrMsg);
        sqlite3_free(zErrMsg);
    } else {
        fprintf(stdout, "Records created successfully\n");
    }

    sqlite3_close(db);

}

void sendloginconf(char *decision, int curr){
    if(send(curr,decision,strlen(decision),0) < 0) {
        printf("Sending failure\n");
    }else{
        printf("I have sent in the decision!\n");
    }
    recvmg(&curr); 
}

void recvmg(void* client_sock){
    printf("Rozpoczynam oczekiwanie na wiadomosci\n");
    Message new_message;
    Request new_request;
    Credentials new_credentials;
    printf("tutaj\n");
    int sock = *((int *)client_sock);
    printf("CS: %d\n", sock);
    char msg[500];
    int len;
    int decision;

    while((len = recv(sock,msg,500,0)) > 0) {
        if(msg[0] == 'r'){
            new_request = decodeRequest(msg);
            printf("%s", new_request.name);
            int decision = check_user_in_database(new_request.name);;
            int totalLen = strlen(new_request.name) + 7;
            char *output = allocMemForString(totalLen); 
            encodeRequest(output, new_request, decision);
            if(send(sock,output,totalLen,0) < 0) {
                printf("Sending failure\n");
            }
            else{
                printf("I have forwarded the message!\n\n");
            }
        }
        else if(msg[0] == 's'){
            new_credentials = decodeCredentials(msg, true);
            decision = register_user(new_credentials, sock);
            printf("Decision about register: %d", decision);
            if(decision){
                if(send(sock,"1\n",2,0) < 0) {
                    printf("Sending failure\n");
                }
                else{
                    printf("I have forwarded the message!\n\n");
                }
            }else if(decision == 0){
                if(send(sock,"0\n",2,0) < 0) {
                    printf("Sending failure\n");
                }
                else{
                    printf("I have forwarded the message!\n\n");
                }
            }
            
        }else if(msg[0] == 'l'){
            new_credentials = decodeCredentials(msg, false);
            printf("Login: %s\n", new_credentials.login);
            printf("Haslo: %s\n", new_credentials.password);
            int checking_result =  check_login_details(new_credentials);
            if(checking_result == 2){
                update_user_socket(new_credentials.login, sock);
                sendloginconf("2\n", sock);
            }
            else if(checking_result == 1){
                sendloginconf("1\n", sock);
            }
            else if(checking_result == 0){
                sendloginconf("0\n", sock);
            }
        }
        else if(msg[0] == 'q'){
            number_of_clients--;
        }
        else{
            new_message = decode(msg);
            printf("Sender: %s\n", new_message.sender);
            printf("Receiver: %s\n",new_message.receiver);
            printf("Content: %s",new_message.content);
            sendtoreceiver(new_message.sender, new_message.receiver, new_message.content);
        }
    }

}

int main(int argc, char *argv[]){
    struct sockaddr_in ServerIp;
    pthread_t recvt;
    int sock=0 , Client_sock=0;
    char login_message[100];
    char login_message_text[] = "\nProsze o podanie loginu oraz hasla:\n";
    strcpy(login_message, login_message_text);

    ServerIp.sin_family = AF_INET;
    ServerIp.sin_port = htons(atoi(argv[2]));
    printf("%d\n", atoi(argv[2]));
    printf("%s\n", argv[1]);
    ServerIp.sin_addr.s_addr = inet_addr(argv[1]);
    sock = socket( AF_INET , SOCK_STREAM, 0 );

    if( bind( sock, (struct sockaddr *)&ServerIp, sizeof(ServerIp)) == -1 ){
        printf("\nCannot bind, error! \n");
    }
    else{
        printf("Server started\n");
    }

    if( listen( sock ,20 ) == -1 ){
        printf("listening failed\n");
    }

    while(1){

        if(number_of_clients < MAX_NUMBER_OF_CLIENTS){
            if( (Client_sock = accept(sock, (struct sockaddr *)NULL,NULL)) < 0 ){
                        printf("Accept failed\n");
            }
        }
        
        pthread_mutex_lock(&mutex);

        pthread_create(&recvt, NULL, (void *)recvmg, &Client_sock);

        number_of_clients++;


        pthread_mutex_unlock(&mutex);
    }
    return 0;

}