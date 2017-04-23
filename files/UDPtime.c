/* UDPtime.c - main */
/* Modified by Krishna Sivalingam, IITM */
/* Combined multiple function definitions from different files into this file */
/* Jan. 16, 2017 */

#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>

#include <netdb.h>

#include <unistd.h>
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <time.h>
#include <sys/errno.h>
#define	BUFSIZE 64
#define h_addr h_addr_list[0] /* for backward compatibility */
#define	UNIXEPOCH	2208988800UL	/* UNIX epoch, in UCT secs	*/
#define	MSG		"what time is it?\n"

extern int	errno;

#ifndef	INADDR_NONE
#define	INADDR_NONE	0xffffffff
#endif	/* INADDR_NONE */

int	errexit(const char *format, ...);

/*------------------------------------------------------------------------
 * connectsockUDP - allocate & connect a socket using UDP
 *------------------------------------------------------------------------
 */
int
connectsockUDP(const char *host, const char *service)
/*
 * Arguments:
 *      host      - name of host to which connection is desired
 *      service   - service associated with the desired port
 */
{
	struct hostent	*phe;	/* pointer to host information entry	*/
	struct servent	*pse;	/* pointer to service information entry	*/
	struct protoent *ppe;	/* pointer to protocol information entry*/
	struct sockaddr_in sin;	/* an Internet endpoint address		*/
	int	s, type;	/* socket descriptor and socket type	*/
	char transport[10];

	strcpy(transport, "udp");

	memset(&sin, 0, sizeof(sin));
	sin.sin_family = AF_INET;

	/* Map service name to port number */
	if ( (pse = getservbyname(service, transport)) != NULL )
		sin.sin_port = pse->s_port;
	else if ((sin.sin_port=htons((unsigned short)atoi(service))) == 0)
		errexit("can't get \"%s\" service entry\n", service);

	/* Map host name to IP address, allowing for dotted decimal */
	if ( (phe = gethostbyname(host)) != NULL )
	  memcpy(&sin.sin_addr, phe->h_addr, phe->h_length);
	else if ( (sin.sin_addr.s_addr = inet_addr(host)) == INADDR_NONE )
	  errexit("can't get \"%s\" host entry\n", host);

	/* Map transport protocol name to protocol number */
	if ( (ppe = getprotobyname(transport)) == NULL)
	  errexit("can't get \"%s\" protocol entry\n", transport);
	
	type = SOCK_DGRAM;

	/* Allocate a socket */
	s = socket(PF_INET, type, ppe->p_proto);
	if (s < 0)
	  errexit("can't create socket: %s\n", strerror(errno));
	
	/* Connect the socket */
	if (connect(s, (struct sockaddr *)&sin, sizeof(sin)) < 0)
	  errexit("can't connect to %s.%s: %s\n", host, service,
		  strerror(errno));
	return s;
}

/*------------------------------------------------------------------------
 * main - UDP client for TIME service that prints the resulting time
 *------------------------------------------------------------------------
 */
int
main(int argc, char *argv[])
{
	char	*host = "localhost";	/* host to use if none supplied	*/
	char	*service = "time";	/* default service name		*/
	time_t	now;			/* 32-bit integer to hold time	*/ 
	int	s, n;			/* socket descriptor, read count*/

	switch (argc) {
	case 1:
		host = "localhost";
		break;
	case 3:
		service = argv[2];
		/* FALL THROUGH */
	case 2:
		host = argv[1];
		break;
	default:
		fprintf(stderr, "usage: UDPtime [host [port]]\n");
		exit(1);
	}

	s = connectsockUDP(host, service);

	(void) write(s, MSG, strlen(MSG));

	/* Read the time */

	n = read(s, (char *)&now, sizeof(now));
	if (n < 0)
		errexit("read failed: %s\n", strerror(errno));
	now = ntohl((unsigned long)now);	/* put in host order	*/
	now -= UNIXEPOCH;		/* convert UCT to UNIX epoch	*/
	printf("%s", ctime(&now));
	exit(0);
}
