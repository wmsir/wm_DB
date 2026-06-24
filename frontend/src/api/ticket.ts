export const downloadTicketAttachment = async (ticketId: string): Promise<string> => {
    try {
        // Mock response for architecture demo to prevent API failures
        // const response = await axios.get(`/api/v1/ticket/${ticketId}/download-url`);
        // return response.data.url;

        return Promise.resolve(`http://localhost:9000/wmdb-attachments/mock-file-${ticketId}.sql?X-Amz-Algorithm=AWS4-HMAC-SHA256&Expires=300`);
    } catch (error) {
        throw new Error('Failed to fetch pre-signed URL');
    }
}
