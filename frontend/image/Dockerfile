FROM docker.io/nginx:alpine

# Copy the custom nginx configuration
COPY image/nginx.conf /etc/nginx/conf.d/default.conf

# Copy the static export output
COPY out /usr/share/nginx/html

# Expose port 80
EXPOSE 80

# Start nginx
CMD ["nginx", "-g", "daemon off;"]
