//
// Created by n08i40k on 19.04.2025.
//

#include "daemonize.h"

#include <cstdio>
#include <cstdlib>
#include <unistd.h>

void
daemonize()
{
    if (daemon(0, 1) < 0) {
        perror("daemon failed");
        exit(EXIT_FAILURE);
    }

    printf("Daemonized successfully!\n");
    fflush(stdout);
}
