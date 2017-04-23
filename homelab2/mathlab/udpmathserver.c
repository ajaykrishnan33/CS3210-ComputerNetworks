/*
 * tcpserver.c - A simple TCP echo server
 * usage: tcpserver <port>
 */

#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>
#include <netdb.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <math.h>

#define BUFSIZE 1024

#if 0
/*
 * Structs exported from in.h
 */

/* Internet address */
struct in_addr {
    unsigned int s_addr;
};

/* Internet style socket address */
struct sockaddr_in  {
    unsigned short int sin_family; /* Address family */
    unsigned short int sin_port;   /* Port number */
    struct in_addr sin_addr;   /* IP address */
    unsigned char sin_zero[...];   /* Pad to size of 'struct sockaddr' */
};

/*
 * Struct exported from netdb.h
 */

/* Domain name service (DNS) host entry */
struct hostent {
    char    *h_name;        /* official name of host */
    char    **h_aliases;    /* alias list */
    int     h_addrtype;     /* host address type */
    int     h_length;       /* length of address */
    char    **h_addr_list;  /* list of addresses */
}
#endif

/*
 * error - wrapper for perror
 */
void error(char *msg) {
    perror(msg);
    exit(1);
}

char* compute(char* input, int len) {
    char delimiter[2] = " ";
    char *operation, *token;
    int op1, op2;
    operation = strtok(input, delimiter);
    if (operation == NULL)
        return "\n\nError:\n\t No operation specified\n";

    token = strtok(NULL, delimiter);
    if (token == NULL)
        return "\n\nError:\n\t No operands provided\n";

    int len1 = strlen(token), len2;

    op1 = atoi(token);

    token = strtok(NULL, delimiter);
    if (token == NULL)
        return "\n\nError:\n\t Too few operands provided\n"; // too few operands

    len2 = strlen(token);

    op2 = atoi(token);

    token = strtok(NULL, delimiter);
    if (token != NULL)
        return "\n\nError:\n\t Too many operands provided\n"; // too many operands

    char* result = (char*)malloc((len1 + len2 + 1) * sizeof(char));
    if (strcmp(operation, "add") == 0) {
        sprintf(result, "%d", op1 + op2);
    }
    else if (strcmp(operation, "sub") == 0) {
        sprintf(result, "%d", op1 - op2);
    }
    else if (strcmp(operation, "mul") == 0) {
        sprintf(result, "%d", op1 * op2);
    }
    else if (strcmp(operation, "div") == 0) {
        sprintf(result, "%f", ((double)op1) / ((double)op2));
    }
    else if (strcmp(operation, "exp") == 0) {
        sprintf(result, "%ld", (long int)pow(op1, op2));
    }
    else
    {
        return "\n\nError:\n\t Incorrect operator. The allowed operators are: add, sub, mul, div, exp\n";
    }

    return result;

}

int main(int argc, char **argv) {
    int parentfd; /* parent socket */
    int childfd; /* child socket */
    int portno; /* port to listen on */
    int clientlen; /* byte size of client's address */
    struct sockaddr_in serveraddr; /* server's addr */
    struct sockaddr_in clientaddr; /* client addr */
    struct hostent *hostp; /* client host info */
    char buf[BUFSIZE]; /* message buffer */
    char result[BUFSIZE]; /* message buffer */
    char *hostaddrp; /* dotted decimal host addr string */
    int optval; /* flag value for setsockopt */
    int n; /* message byte size */

    /*
     * check command line arguments
     */
    if (argc != 2) {
        fprintf(stderr, "usage: %s <port>\n", argv[0]);
        exit(1);
    }
    portno = atoi(argv[1]);

    /*
     * socket: create the parent socket
     */
    parentfd = socket(AF_INET, SOCK_DGRAM, 0);
    if (parentfd < 0)
        error("ERROR opening socket");

    /* setsockopt: Handy debugging trick that lets
     * us rerun the server immediately after we kill it;
     * otherwise we have to wait about 20 secs.
     * Eliminates "ERROR on binding: Address already in use" error.
     */
    optval = 1;
    setsockopt(parentfd, SOL_SOCKET, SO_REUSEADDR,
               (const void *)&optval , sizeof(int));

    /*
     * build the server's Internet address
     */
    bzero((char *) &serveraddr, sizeof(serveraddr));

    /* this is an Internet address */
    serveraddr.sin_family = AF_INET;

    /* let the system figure out our IP address */
    serveraddr.sin_addr.s_addr = htonl(INADDR_ANY);

    /* this is the port we will listen on */
    serveraddr.sin_port = htons((unsigned short)portno);

    /*
     * bind: associate the parent socket with a port
     */
    if (bind(parentfd, (struct sockaddr *) &serveraddr,
             sizeof(serveraddr)) < 0)
        error("ERROR on binding");

    /*
     * main loop: wait for a connection request, echo input line,
     * then close connection.
     */
    clientlen = sizeof(clientaddr);
    char* temp;
    while (1) {

        childfd = parentfd;
        /*
         * read: read input string from the client
         */
        bzero(buf, BUFSIZE);

        n = recvfrom(childfd, buf, BUFSIZE, 0, (struct sockaddr *)&clientaddr, (socklen_t*)&clientlen);

        // hostp = gethostbyaddr((const char *)&clientaddr.sin_addr.s_addr,
        //     sizeof(clientaddr.sin_addr.s_addr), AF_INET);
        // if (hostp == NULL)
        //   error("ERROR on gethostbyaddr");
        // hostaddrp = inet_ntoa(clientaddr.sin_addr);
        // if (hostaddrp == NULL)
        //   error("ERROR on inet_ntoa\n");
        // printf("server established connection with %s (%s:%d)\n",
        //  hostp->h_name, hostaddrp, clientaddr.sin_port);

        if (n < 0)
            error("ERROR reading from socket");
        printf("Received: %s\n", buf);

        /*
         * write: echo the input string back to the client
         */

        temp = compute(buf, n);

        n = sendto(childfd, temp, strlen(temp) + 1, 0, (struct sockaddr *)&clientaddr, clientlen);

        if (n < 0)
            error("ERROR writing to socket");

    }
}
