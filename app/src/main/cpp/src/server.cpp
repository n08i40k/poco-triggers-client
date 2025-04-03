//
// Created by n08i40k on 03.04.2025.
//

#include "server.h"

#include <cstdio>
#include <sys/socket.h>
#include <unistd.h>

server::server(const std::uint16_t port)
    : fd_(socket(AF_INET, SOCK_STREAM, 0))
{
    if (fd_ == 0) {
        perror("Failed to create socket");
        return;
    }

    // setup recv time
    timeval recv_opt{};
    recv_opt.tv_sec  = 5;
    recv_opt.tv_usec = 0;

    if (setsockopt(fd_, SOL_SOCKET, SO_RCVTIMEO, &recv_opt, sizeof(recv_opt))
        != 0) {
        perror("Failed to set socket recv option");
        close(fd_);
        fd_ = -1;
        return;
    }

    // setup reuse addr
    constexpr int reuse_opt = 1;

    if (setsockopt(fd_, SOL_SOCKET, SO_REUSEADDR, &reuse_opt, sizeof(reuse_opt))
        != 0) {
        perror("Failed to set socket reuse address option");
        close(fd_);
        fd_ = -1;
        return;
    }

    address_.sin_family      = AF_INET;
    address_.sin_addr.s_addr = INADDR_ANY;
    address_.sin_port        = htons(port);

    if (bind(fd_, reinterpret_cast<sockaddr*>(&address_), sizeof(address_)) < 0) {
        perror("Failed to bind socket");
        close(fd_);
        fd_ = -1;
        return;
    }

    if (listen(fd_, SOMAXCONN) < 0) {
        perror("Failed to listen on socket");
        close(fd_);
        fd_ = -1;
        return;
    }
}

server::~server()
{
    if (fd_ > 0)
        close(fd_);
}

void
server::work_thread(const std::function<void(client_message&)>& on_message)
{
    auto* const addr     = reinterpret_cast<struct sockaddr*>(&address_);
    socklen_t   addr_len = sizeof(address_);

    while (true) {
        const int sock = accept(fd_, addr, &addr_len);

        if (sock < 0) {
            if (fd_ == 0)
                return;

            if (errno == EAGAIN)
                continue;

            perror("Failed to accept connection");
            continue;
        }

        printf("New client!\n");
        fflush(stdout);

        client_message message{};

        while (true) {
            const auto n = read(sock, &message, sizeof(message));

            if (n == 0) {
                printf("Client disconnected!\n");
                fflush(stdout);

                break;
            }

            if (n != sizeof(message))
                continue;

            on_message(message);
        }

        close(sock);
    }
}
